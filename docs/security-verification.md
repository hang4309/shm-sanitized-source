# Rate-limit identity verification

## Scope and findings

The original limiter selected the first `X-Forwarded-For` entry without checking the socket peer. A direct caller could rotate that value to obtain separate buckets. The first correction introduced an explicit proxy allowlist. Independent review then found three additional problems:

1. A character allowlist followed by `InetAddress.getByName` could resolve hostnames such as `cafe`; the promised numeric-IP-only validation was false. This required a hostname in proxy configuration or a value provided by an already trusted proxy, not an ordinary untrusted caller.
2. A preceding Spring `ForwardedHeaderFilter` could replace `getRemoteAddr()` and the apparent URI. That filter was not enabled in the committed default configuration; the finding concerned deployments that enabled it or inserted it manually.
3. The 12-request regression assumed completion within 100 milliseconds. A 250 ms scheduling pause let a correct 10/s token bucket accept all 12 requests, making the test depend on machine timing.

## Implemented controls

`IpAddressLiteral` parses strict dotted IPv4 and IPv6 directly into bytes and uses only `InetAddress.getByAddress` for canonical formatting. No name resolver is called. IPv4-mapped IPv6 shares its IPv4 identity. Hostnames, CIDRs, ports, bracketed addresses, IPv6 zone identifiers, shortened IPv4 and leading-zero IPv4 are rejected.

`ApiRateLimitFilter` runs at highest filter precedence, unwraps standard servlet request wrappers, and reads the original peer, headers, method and URI. Invalid peers or excessive/malformed wrapping are rejected. Only configured peers can supply exactly one valid `X-Real-IP`; invalid or repeated values use the proxy peer. The application explicitly sets forwarding strategy `NONE` and rejects `NATIVE`/`FRAMEWORK` settings at startup. Custom native container extensions that alter the peer outside this configuration are unsupported.

Production refill/cleanup timing uses monotonic milliseconds. Tests inject a controlled clock, so neither scheduling pauses nor wall-clock changes affect their accepted/rejected counts.

## Reproduction and evidence

From `shm-backend-fable5-copy`, run:

```sh
./mvnw -B verify
```

On Windows use `.\mvnw.cmd -B verify`. The context test uses H2 in memory with MySQL compatibility; it does not use an operational database. The HTTP tests bind only to `127.0.0.1` on ephemeral ports and stop their servers afterward.

The local run on 2026-10-09 used JDK 25, Maven 3.9.11, and Java 17 bytecode: **87 tests, 0 failures, 0 errors, 0 skipped; executable JAR packaging succeeded**. CI is configured to execute the same Maven verification with JDK 17; a local result does not establish the result of a future CI run. The evidence files are generated under `target/surefire-reports/`; build output is not committed.

| Evidence | What it establishes |
| --- | --- |
| `ApiRateLimitFilterTest` | Fixed-clock burst/refill behavior; direct and trusted identities; invalid configuration and headers; duplicate headers; wrapper/path defense; environment binding; unsafe forwarding settings prevent context startup |
| `IpAddressLiteralTest` | Valid IPv4/IPv6 canonical forms and rejection of hostname, ambiguous and malformed forms |
| `ApiRateLimitHttpTest` | Actual Tomcat HTTP requests over loopback, with and without a preceding forwarding filter: 12 rotated-header requests yield exactly 10 HTTP 200 and 2 HTTP 429 with `Retry-After: 1`; exactly 10 reach the synthetic servlet |
| `ShmBackendApplicationTests` | The application context starts with isolated test database settings |

The HTTP fixture exercises the production limiter and container dispatch with a synthetic servlet. It does not prove the complete production database, dashboard, device integration, authentication, or network deployment. Tests demonstrate the stated rate-limit boundary, not general application security or an open-source license grant.
