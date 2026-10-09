package com.example.shm.common.ratelimit;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Real local sockets/container filter dispatch; time is fixed, so request duration cannot affect the count. */
class ApiRateLimitHttpTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realHttpRotatingHeadersShareOneSocketPeerBucket(boolean precedingForwardedFilter) throws Exception {
        Tomcat server = new Tomcat();
        server.setBaseDir(temporaryDirectory.toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        Context context = server.addContext("", temporaryDirectory.toString());
        AtomicInteger reachedEndpoint = new AtomicInteger();
        Tomcat.addServlet(context, "synthetic-endpoint", new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                reachedEndpoint.incrementAndGet();
                response.setContentType("application/json");
                response.getWriter().write("{\"synthetic\":true}");
            }
        });
        context.addServletMappingDecoded("/*", "synthetic-endpoint");
        if (precedingForwardedFilter) {
            register(context, "forwarded", new ForwardedHeaderFilter());
        }
        register(context, "limiter", new ApiRateLimitFilter("", () -> 0L));
        try {
            server.start();
            URI target = URI.create("http://127.0.0.1:" + server.getConnector().getLocalPort()
                    + "/api/data/strain/latest");
            for (int i = 1; i <= 12; i++) {
                HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection(Proxy.NO_PROXY);
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("X-Forwarded-For", "198.51.100." + i);
                connection.setRequestProperty("X-Real-IP", "192.0.2." + i);
                connection.setRequestProperty("X-Forwarded-Prefix", "/forged");
                try {
                    assertEquals(i <= 10 ? 200 : 429, connection.getResponseCode(), "request " + i);
                    if (i > 10) {
                        assertEquals("1", connection.getHeaderField("Retry-After"));
                    }
                } finally {
                    connection.disconnect();
                }
            }
            assertEquals(10, reachedEndpoint.get());
        } finally {
            server.stop();
            server.destroy();
        }
    }

    private static void register(Context context, String name, jakarta.servlet.Filter filter) {
        FilterDef definition = new FilterDef();
        definition.setFilterName(name);
        definition.setFilter(filter);
        context.addFilterDef(definition);
        FilterMap mapping = new FilterMap();
        mapping.setFilterName(name);
        mapping.addURLPattern("/*");
        context.addFilterMap(mapping);
    }
}
