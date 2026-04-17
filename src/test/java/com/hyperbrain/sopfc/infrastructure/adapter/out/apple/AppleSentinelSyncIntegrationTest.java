package com.hyperbrain.sopfc.infrastructure.adapter.out.apple;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.model.ExecutableStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AppleSentinelSyncIntegrationTest {

    @Autowired
    private AppleSentinelSyncAdapter appleAdapter;

    @Test
    void shouldCreateAndUpdateReminderIniOS() {
        // 1. PUSH CREATE
        CoreExecutable executable = CoreExecutable.builder()
                .name("🚀 SOPFC Apple Test - " + System.currentTimeMillis())
                .status(ExecutableStatus.PENDING)
                .build();

        System.out.println("--- Creating Reminder in iOS ---");
        String externalId = appleAdapter.pushCreate(executable);
        assertNotNull(externalId, "Sentinel should return a valid Reminder ID");
        System.out.println("Created Reminder ID: " + externalId);

        // 2. PUSH UPDATE
        CoreExecutable updateData = executable.toBuilder()
                .status(ExecutableStatus.DONE)
                .build();

        System.out.println("--- Updating Reminder to DONE ---");
        assertDoesNotThrow(() -> appleAdapter.pushUpdate(updateData, externalId));
        
        System.out.println("Apple Reminders integration verified successfully.");
    }
}
