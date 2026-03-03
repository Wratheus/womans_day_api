package com.womansday.api.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Явная настройка лимитов multipart-загрузки (200 МБ на файл, 500 МБ на запрос).
 * Гарантирует применение лимитов независимо от порядка загрузки application.properties.
 */
@Configuration
public class MultipartConfig {

    private static final long MAX_FILE_SIZE_MB = 200;
    private static final long MAX_REQUEST_SIZE_MB = 500;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(MAX_FILE_SIZE_MB));
        factory.setMaxRequestSize(DataSize.ofMegabytes(MAX_REQUEST_SIZE_MB));
        return factory.createMultipartConfig();
    }
}
