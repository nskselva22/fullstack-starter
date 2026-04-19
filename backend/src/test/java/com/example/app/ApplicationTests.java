package com.example.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — Spring context should start with embedded Mongo (Flapdoodle)
 * and no real OAuth credentials required.
 */
@SpringBootTest(properties = {
    "spring.security.oauth2.client.registration.google.client-id=dummy",
    "spring.security.oauth2.client.registration.google.client-secret=dummy"
})
class ApplicationTests {

    @Test
    void contextLoads() {
        // If the application context fails to start, this test fails.
    }
}
