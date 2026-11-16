package com.ivanjiang.regtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.host=",
        "app.jwt.secret=testSecretKeyForJwtSigningWhichIsLongEnough123",
        "aws.region=us-east-1",
        "aws.s3.bucket=test-bucket"
})
class RegTrackerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts cleanly
    }
}
