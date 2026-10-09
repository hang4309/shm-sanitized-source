package com.example.shm.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiRateLimitFilterTest {

    @Test
    void directClientsCannotChooseRateLimitIdentityWithForwardingHeaders() {
        ApiRateLimitFilter filter = new ApiRateLimitFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");
        request.addHeader("X-Real-IP", "198.51.100.2");

        assertEquals("203.0.113.10", filter.resolveClientIp(request));
    }

    @Test
    void rotatingForwardedHeadersCannotBypassThePerPeerBurstLimit() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter("");
        int accepted = 0;
        int rejected = 0;
        for (int i = 1; i <= 12; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data/strain/latest");
            request.setRequestURI("/api/data/strain/latest");
            request.setRemoteAddr("203.0.113.10");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            request.addHeader("X-Real-IP", "192.0.2." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());
            if (response.getStatus() == 429) {
                rejected++;
            } else {
                accepted++;
            }
        }

        assertEquals(10, accepted);
        assertEquals(2, rejected);
    }

    @Test
    void configuredProxyMaySupplyAValidatedRealIp() {
        ApiRateLimitFilter filter = new ApiRateLimitFilter("127.0.0.1, ::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.10");
        request.addHeader("X-Forwarded-For", "198.51.100.11");

        assertEquals("198.51.100.10", filter.resolveClientIp(request));
    }

    @Test
    void invalidForwardedAddressFallsBackToTrustedProxyPeer() {
        ApiRateLimitFilter filter = new ApiRateLimitFilter("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "attacker.example");

        assertEquals("127.0.0.1", filter.resolveClientIp(request));
    }

    @Test
    void invalidTrustedProxyConfigurationFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiRateLimitFilter("proxy.example"));
    }
}
