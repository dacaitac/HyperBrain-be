package com.hyperbrain.sopfc.it.smoke;

import com.hyperbrain.sopfc.SopfcApplication;
import com.hyperbrain.sopfc.it.config.AbstractTestcontainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SopfcApplication.class)
@ActiveProfiles("test")
public class ApplicationStartupSmokeTest extends AbstractTestcontainersIntegrationTest {

    @Test
    @DisplayName("🚀 The application context should load correctly with Kafka and Postgres")
    void contextLoads() {
        // If this method is called, the context loaded successfully
        assertTrue(true, "Application context loaded successfully");
    }
}
