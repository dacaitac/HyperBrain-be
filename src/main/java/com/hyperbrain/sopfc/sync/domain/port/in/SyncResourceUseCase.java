package com.hyperbrain.sopfc.sync.domain.port.in;

import java.util.Map;

public interface SyncResourceUseCase {
    void create(Map<String, Object> data);
    void update(Map<String, Object> data);
    void delete(Map<String, Object> data);
}
