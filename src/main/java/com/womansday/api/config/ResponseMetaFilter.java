package com.womansday.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ResponseMetaFilter extends OncePerRequestFilter {

    @Value("${spring.profiles.active:unknown}")
    private String profile;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Data-Source", profile);

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        response.setHeader("X-Response-Time", duration + "ms");
    }
}
