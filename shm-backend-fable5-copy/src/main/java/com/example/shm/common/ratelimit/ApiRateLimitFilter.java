package com.example.shm.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.function.LongSupplier;

/**
 * Per-endpoint, per-client-IP rate limiting for the hot data APIs.
 *
 * Manual in-memory token bucket: no Redis, no external infrastructure.
 * Buckets are keyed by clientIp|method|path, stored in a ConcurrentHashMap,
 * and consume inside the smallest possible synchronized section. Excess
 * requests are rejected immediately with HTTP 429 (never 500, never a
 * blocked thread) and every rejection is logged.
 *
 * Limits:
 *   POST /api/data/{module}/latest    10 req/s per IP (burst 10)
 *   POST /api/data/{module}/history   10 req/s per IP (burst 10)
 *   POST /api/sensor/os265/raw/upload  5 req/s per IP (burst 150)
 *
 * The upload bucket has a large burst capacity on purpose: OS265 flushes
 * channel files in chunks of roughly 100 lines every ~50 seconds, so the
 * collector legitimately uploads a short burst, then stays idle. The
 * sustained rate stays capped at 5/s. 429 responses carry Retry-After: 1
 * so well-behaved clients (including the collector) may retry after a
 * short delay.
 *
 * Memory safety: each bucket stores lastAccessMillis; buckets idle for
 * more than 5 minutes are pruned lazily at most once per minute. Active
 * client count has no hard cap; this is not a distributed DoS boundary.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);

    private static final Pattern MODULE_DATA_PATTERN = Pattern.compile(
            "^/api/data/(displacement|acceleration|strain|vibration|stress|deflection)/(latest|history)$");
    private static final String UPLOAD_PATH = "/api/sensor/os265/raw/upload";

    private static final RateLimitRule MODULE_DATA_RULE =
            new RateLimitRule("module-data", 10.0, 10);
    private static final RateLimitRule UPLOAD_RULE =
            new RateLimitRule("os265-upload", 5.0, 150);

    private static final long IDLE_EVICT_MILLIS = 5 * 60 * 1000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 60 * 1000L;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMillis;
    private final Set<String> trustedProxyIps;
    private final LongSupplier clockMillis;

    @Autowired
    public ApiRateLimitFilter(@Value("${shm.rate-limit.trusted-proxy-ips:}") String configuredProxyIps,
                              @Value("${server.forward-headers-strategy:NONE}") String forwardingStrategy) {
        this(configuredProxyIps, forwardingStrategy, () -> System.nanoTime() / 1_000_000L);
    }

    ApiRateLimitFilter(String configuredProxyIps, LongSupplier clockMillis) {
        this(configuredProxyIps, "NONE", clockMillis);
    }

    private ApiRateLimitFilter(String configuredProxyIps, String forwardingStrategy, LongSupplier clockMillis) {
        if (!"NONE".equalsIgnoreCase(forwardingStrategy.trim())) {
            throw new IllegalArgumentException("The rate limiter requires server.forward-headers-strategy=NONE; "
                    + "configure shm.rate-limit.trusted-proxy-ips and overwrite X-Real-IP at the proxy instead");
        }
        this.clockMillis = Objects.requireNonNull(clockMillis);
        this.lastCleanupMillis = new AtomicLong(clockMillis.getAsLong());
        Set<String> parsed = new HashSet<>();
        Arrays.stream(configuredProxyIps.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(value -> {
                    String normalized = IpAddressLiteral.canonical(value);
                    if (normalized == null) {
                        throw new IllegalArgumentException("Invalid trusted proxy IP address: " + value);
                    }
                    parsed.add(normalized);
                });
        this.trustedProxyIps = Set.copyOf(parsed);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest original = originalRequest(request);
        if (original == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported request wrapper");
            return;
        }
        String path = original.getRequestURI();
        String method = original.getMethod();

        RateLimitRule rule = resolveRule(method, path);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = clockMillis.getAsLong();
        maybeCleanup(now);

        String clientIp = resolveClientIp(request);
        if (clientIp == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid client socket address");
            return;
        }
        String key = clientIp + "|" + method + "|" + path;
        TokenBucket bucket = buckets.computeIfAbsent(
                key, ignored -> new TokenBucket(rule.burstCapacity(), rule.refillPerSecond(), now));

        if (bucket.tryConsume(now)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit rejected: clientIp={} method={} endpoint={} limit={} rate={}rps burst={} tickMs={}",
                clientIp, method, path, rule.name(), rule.refillPerSecond(), rule.burstCapacity(), now);

        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":429,\"message\":\"Rate limit exceeded for endpoint " + path + "\",\"data\":null}");
    }

    private RateLimitRule resolveRule(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        if (UPLOAD_PATH.equals(path)) {
            return UPLOAD_RULE;
        }
        if (MODULE_DATA_PATTERN.matcher(path).matches()) {
            return MODULE_DATA_RULE;
        }
        return null;
    }

    /**
     * Trust X-Real-IP only when the socket peer is an explicitly configured
     * reverse proxy. Read the original servlet request so a preceding framework
     * wrapper cannot replace the peer with an untrusted forwarding header.
     */
    String resolveClientIp(HttpServletRequest request) {
        HttpServletRequest original = originalRequest(request);
        if (original == null) {
            return null;
        }
        String remoteIp = IpAddressLiteral.canonical(original.getRemoteAddr());
        if (remoteIp == null) {
            return null;
        }
        if (trustedProxyIps.contains(remoteIp)) {
            Enumeration<String> headers = original.getHeaders("X-Real-IP");
            String value = headers != null && headers.hasMoreElements() ? headers.nextElement() : null;
            String forwardedIp = headers != null && headers.hasMoreElements()
                    ? null : IpAddressLiteral.canonical(value);
            if (forwardedIp != null) {
                return forwardedIp;
            }
        }
        return remoteIp;
    }

    private static HttpServletRequest originalRequest(HttpServletRequest request) {
        ServletRequest current = request;
        Set<ServletRequest> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current instanceof ServletRequestWrapper wrapper) {
            if (seen.size() >= 32 || !seen.add(current)) {
                return null;
            }
            current = wrapper.getRequest();
        }
        return current instanceof HttpServletRequest original ? original : null;
    }

    /**
     * Lazy pruning of idle buckets, at most once per minute.
     */
    private void maybeCleanup(long now) {
        long last = lastCleanupMillis.get();
        if (now - last < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        if (!lastCleanupMillis.compareAndSet(last, now)) {
            return;
        }
        buckets.entrySet().removeIf(
                entry -> now - entry.getValue().lastAccessMillis() > IDLE_EVICT_MILLIS);
    }

    private record RateLimitRule(String name, double refillPerSecond, int burstCapacity) {
    }

    /**
     * Minimal token bucket. Refill and consume happen inside one small
     * synchronized section; lastAccess is volatile for lock-free pruning.
     */
    private static final class TokenBucket {
        private final int capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillMillis;
        private volatile long lastAccess;

        TokenBucket(int capacity, double refillPerSecond, long now) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillMillis = now;
            this.lastAccess = now;
        }

        synchronized boolean tryConsume(long now) {
            lastAccess = now;
            if (now > lastRefillMillis) {
                tokens = Math.min(capacity,
                        tokens + (now - lastRefillMillis) / 1000.0 * refillPerSecond);
                lastRefillMillis = now;
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        long lastAccessMillis() {
            return lastAccess;
        }
    }
}
