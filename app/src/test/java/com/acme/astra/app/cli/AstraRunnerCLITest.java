package com.acme.astra.app.cli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the CLI application.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "artifactory.url=http://mock-artifactory.com/repo",
    "artifactory.user=test-user",
    "artifactory.password=test-password"
})
class AstraRunnerCLITest {
    
    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }
}