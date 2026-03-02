package com.courseupload.config;

import com.courseupload.service.LocalStorageService;
import com.courseupload.service.S3StorageService;
import com.courseupload.service.StorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selects the active StorageService bean based on app.storage.strategy.
 * "local" → LocalStorageService (default)
 * "s3"    → S3StorageService    (AWS S3 Bonus)
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.strategy:local}")
    private String storageStrategy;

    @Bean
    @Primary
    public StorageService activeStorageService(
            @Qualifier("localStorageService") LocalStorageService localStorageService,
            @Qualifier("s3StorageService") S3StorageService s3StorageService) {

        if ("s3".equalsIgnoreCase(storageStrategy)) {
            return s3StorageService;
        }
        return localStorageService;
    }
}
