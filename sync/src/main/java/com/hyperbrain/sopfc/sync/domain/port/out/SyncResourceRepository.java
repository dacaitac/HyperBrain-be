package com.hyperbrain.sopfc.sync.domain.port.out;

import java.util.Map;

public interface SyncResourceRepository {
    void save(String fileName, Map<String, Object> data);
}
