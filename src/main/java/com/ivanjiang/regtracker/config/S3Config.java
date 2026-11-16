package com.ivanjiang.regtracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        try {
            return S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        } catch (Exception e) {
            log.warn("Could not initialize S3Client — AWS credentials not available. S3 operations will be mocked. Error: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public S3Presigner s3Presigner() {
        try {
            return S3Presigner.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        } catch (Exception e) {
            log.warn("Could not initialize S3Presigner — presigned URLs will be mocked. Error: {}", e.getMessage());
            return null;
        }
    }
}
