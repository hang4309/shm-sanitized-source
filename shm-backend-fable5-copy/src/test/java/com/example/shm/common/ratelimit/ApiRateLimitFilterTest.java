package com.example.shm.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ApiRateLimitFilterTest {
    private static ApiRateLimitFilter filter(String proxies) {
        return new ApiRateLimitFilter(proxies, () -> 0L);
    }

    static MockHttpServletRequest request(String peer, int sequence) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data/strain/latest");
        request.setRequestURI("/api/data/strain/latest");
        request.setRemoteAddr(peer);
        request.addHeader("X-Forwarded-For", "198.51.100." + sequence);
        request.addHeader("X-Real-IP", "192.0.2." + sequence);
        return request;
    }

    @Test
    void directClientsCannotChooseRateLimitIdentityWithForwardingHeaders() {
        assertEquals("203.0.113.10", filter("").resolveClientIp(request("203.0.113.10", 1)));
    }

    @Test
    void rotatingForwardedHeadersCannotBypassThePerPeerBurstLimit() throws Exception {
        ApiRateLimitFilter filter = filter("");
        AtomicInteger reachedEndpoint = new AtomicInteger();
        for (int i = 1; i <= 12; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("203.0.113.10", i), response,
                    (req, res) -> reachedEndpoint.incrementAndGet());
            assertEquals(i <= 10 ? 200 : 429, response.getStatus(), "request " + i);
            if (i > 10) {
                assertEquals("1", response.getHeader("Retry-After"));
            }
        }
        assertEquals(10, reachedEndpoint.get());
    }

    @Test
    void refillsExactlyOneTokenAfterOneHundredMilliseconds() throws Exception {
        AtomicLong clock = new AtomicLong();
        ApiRateLimitFilter filter = new ApiRateLimitFilter("", clock::get);
        for (int i = 0; i < 10; i++) {
            filter.doFilter(request("203.0.113.10", i), new MockHttpServletResponse(), new MockFilterChain());
        }
        clock.set(99);
        MockHttpServletResponse before = new MockHttpServletResponse();
        filter.doFilter(request("203.0.113.10", 11), before, new MockFilterChain());
        assertEquals(429, before.getStatus());
        clock.set(100);
        MockHttpServletResponse after = new MockHttpServletResponse();
        filter.doFilter(request("203.0.113.10", 12), after, new MockFilterChain());
        assertEquals(200, after.getStatus());
    }

    @Test
    void configuredProxyMaySupplyAValidatedRealIp() {
        assertEquals("192.0.2.1", filter("127.0.0.1, ::1").resolveClientIp(request("127.0.0.1", 1)));
        assertEquals("192.0.2.1", filter("::1").resolveClientIp(request("0:0:0:0:0:0:0:1", 1)));
        assertEquals("192.0.2.1", filter("127.0.0.1").resolveClientIp(request("::ffff:127.0.0.1", 1)));
    }

    @Test
    void separateDirectPeersAndTrustedClientIpsHaveSeparateBuckets() throws Exception {
        for (String proxies : new String[]{"", "127.0.0.1"}) {
            ApiRateLimitFilter filter = filter(proxies);
            for (int i = 1; i <= 12; i++) {
                MockHttpServletRequest request = request(proxies.isEmpty() ? "203.0.113." + i : "127.0.0.1", i);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, new MockFilterChain());
                assertEquals(200, response.getStatus());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafe", "face", "dead.beef", "proxy.example", "127.1", "2130706433",
            "127.00.0.1", "256.0.0.1", "127.0.0.1/32", "127.0.0.1:80", "[::1]", "fe80::1%eth0",
            "1::2::3", ":::", "::ffff:127.01.0.1"})
    void invalidTrustedProxyConfigurationFailsFast(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> filter(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "cafe", "attacker.example", "127.1", "192.0.2.1, 192.0.2.2",
            "[::1]", ":::", " 192.0.2.1", "192.0.2.1 ", "::ffff:127.01.0.1"})
    void invalidForwardedAddressFallsBackToTrustedProxyPeer(String invalid) {
        MockHttpServletRequest request = request("127.0.0.1", 1);
        request.removeHeader("X-Real-IP");
        request.addHeader("X-Real-IP", invalid);
        assertEquals("127.0.0.1", filter("127.0.0.1").resolveClientIp(request));
    }

    @Test
    void missingRealIpDoesNotFallBackToForwardedFor() {
        MockHttpServletRequest request = request("127.0.0.1", 1);
        request.removeHeader("X-Real-IP");
        assertEquals("127.0.0.1", filter("127.0.0.1").resolveClientIp(request));
    }

    @Test
    void repeatedRealIpHeadersFallBackToProxyPeer() {
        MockHttpServletRequest request = request("127.0.0.1", 1);
        request.addHeader("X-Real-IP", "192.0.2.99");
        assertEquals("127.0.0.1", filter("127.0.0.1").resolveClientIp(request));
    }

    @Test
    void malformedSocketAddressFailsClosed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter("").doFilter(request("cafe", 1), response, (req, res) -> fail("endpoint must not run"));
        assertEquals(400, response.getStatus());
    }

    @Test
    void precedingForwardedHeaderFilterCannotChangeIdentityOrProtectedPath() throws Exception {
        ApiRateLimitFilter filter = filter("");
        for (int i = 1; i <= 12; i++) {
            MockHttpServletRequest original = request("203.0.113.10", i);
            original.addHeader("X-Forwarded-Prefix", "/forged");
            MockHttpServletResponse response = new MockHttpServletResponse();
            new ForwardedHeaderFilter().doFilter(original, response, (req, res) -> {
                assertNotEquals(original.getRemoteAddr(), req.getRemoteAddr());
                assertEquals("203.0.113.10", filter.resolveClientIp((HttpServletRequest) req));
                filter.doFilter(req, res, new MockFilterChain());
            });
            assertEquals(i <= 10 ? 200 : 429, response.getStatus());
        }
    }

    @Test
    void excessiveWrappingFailsClosed() throws Exception {
        HttpServletRequest wrapped = request("203.0.113.10", 1);
        for (int i = 0; i < 33; i++) {
            wrapped = new HttpServletRequestWrapper(wrapped);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter("").doFilter(wrapped, response, (req, res) -> fail("endpoint must not run"));
        assertEquals(400, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NATIVE", "FRAMEWORK", "native", "framework", ""})
    void forwardingStrategiesThatRewriteTheSocketPeerAreRejected(String strategy) {
        assertThrows(IllegalArgumentException.class, () -> new ApiRateLimitFilter("", strategy));
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "framework"})
    void unsafeForwardingEnvironmentSettingPreventsContextStartup(String strategy) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env",
                    Map.of("SERVER_FORWARD_HEADERS_STRATEGY", strategy)));
            context.register(ApiRateLimitFilter.class);
            Exception failure = assertThrows(org.springframework.beans.factory.BeanCreationException.class,
                    context::refresh);
            assertTrue(failure.getMessage().contains("ApiRateLimitFilter"));
        }
    }

    @Test
    void environmentVariableBindsToTrustedProxyConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env",
                    Map.of("SHM_RATE_LIMIT_TRUSTED_PROXY_IPS", "127.0.0.1")));
            context.register(ApiRateLimitFilter.class);
            context.refresh();
            assertEquals("192.0.2.1", context.getBean(ApiRateLimitFilter.class)
                    .resolveClientIp(request("127.0.0.1", 1)));
        }
    }
}
