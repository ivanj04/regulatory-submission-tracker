package com.ivanjiang.regtracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;

@Slf4j
@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3Service(S3Presigner s3Presigner,
                     @Value("${aws.s3.bucket:reg-submissions-local}") String bucket) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /**
     * Generate a presigned PUT URL for uploading a document.
     * Falls back to a mock URL if S3 is unavailable.
     */
    public String generatePresignedUploadUrl(String key, String contentType) {
        if (s3Presigner == null) {
            log.warn("S3Presigner not available — returning mock upload URL for key: {}", key);
            return "http://localhost:4566/" + bucket + "/" + key + "?mock=upload";
        }
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(putRequest)
                    .build();
            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Failed to generate presigned upload URL for key {}: {} — returning mock URL", key, e.getMessage());
            return "http://localhost:4566/" + bucket + "/" + key + "?mock=upload";
        }
    }

    /**
     * Generate a presigned GET URL for downloading a document.
     * Falls back to a mock URL if S3 is unavailable.
     */
    public String generatePresignedDownloadUrl(String key) {
        if (s3Presigner == null) {
            log.warn("S3Presigner not available — returning mock download URL for key: {}", key);
            return "http://localhost:4566/" + bucket + "/" + key + "?mock=download";
        }
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(getRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Failed to generate presigned download URL for key {}: {} — returning mock URL", key, e.getMessage());
            return "http://localhost:4566/" + bucket + "/" + key + "?mock=download";
        }
    }

    public String buildS3Key(String submissionId, String documentId, String fileName) {
        return String.format("submissions/%s/%s/%s", submissionId, documentId, fileName);
    }
}
