package com.womansday.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );
    private static final Pattern LOGIN_PATTERN = Pattern.compile("\"login\"\\s*:\\s*\"([^\"]{1,50})\"");

    private final Cache<String, AtomicInteger> attemptCounts = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW)
            .build();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        int contentLength = request.getContentLength();
        if (contentLength > 4096) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Request body too large.\"}");
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        String login = extractLogin(body);

        if (login != null) {
            AtomicInteger counter = attemptCounts.get(login, k -> new AtomicInteger(0));
            if (counter.incrementAndGet() > MAX_ATTEMPTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !RATE_LIMITED_PATHS.contains(request.getServletPath());
    }

    private String extractLogin(byte[] body) {
        if (body.length == 0) return null;
        Matcher m = LOGIN_PATTERN.matcher(new String(body, StandardCharsets.UTF_8));
        return m.find() ? m.group(1).toLowerCase() : null;
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isReady() { return true; }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public void setReadListener(ReadListener listener) { }
                @Override public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
