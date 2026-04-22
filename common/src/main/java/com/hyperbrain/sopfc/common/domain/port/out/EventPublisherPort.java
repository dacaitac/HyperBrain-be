package com.hyperbrain.sopfc.common.domain.port.out;

import com.hyperbrain.sopfc.common.domain.event.ExecutableDeletedEvent.MappingInfo;
import java.util.List;
import java.util.UUID;

public interface EventPublisherPort {
    void publishExecutableStatusChanged(UUID executableId, String newStatus, String sourceSystem);
    void publishExecutableDeleted(UUID executableId, String sourceSystem, List<MappingInfo> mappings);
}
