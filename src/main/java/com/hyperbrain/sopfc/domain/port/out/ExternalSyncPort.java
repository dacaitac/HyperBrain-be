package com.hyperbrain.sopfc.domain.port.out;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import java.util.List;
import java.util.Optional;

/**
 * Port for interacting with external productivity systems (Notion, iOS Reminders/Calendar).
 */
public interface ExternalSyncPort {
    
    /**
     * Result wrapper for external synchronization.
     */
    record ExternalSyncResult(CoreExecutable executable, String externalId) {}

    /**
     * Retrieves the external representation of an executable by its external ID.
     */
    Optional<ExternalSyncResult> fetchById(String externalId);
    
    /**
     * Retrieves all updated or new items from the external system since the last sync.
     */
    List<ExternalSyncResult> fetchDelta();
    
    /**
     * Updates an existing item in the external system.
     */
    void pushUpdate(CoreExecutable executable, String externalId);
    
    /**
     * Creates a new item in the external system.
     * @return the new external ID.
     */
    String pushCreate(CoreExecutable executable);

    /**
     * Deletes an item in the external system.
     */
    void pushDelete(String externalId);

    /**
     * Identifies which system this port handles (e.g., "NOTION", "APPLE").
     */
    String getSystemIdentifier();
}
