package com.hyperbrain.sopfc.infrastructure.adapter.in.web;

import com.hyperbrain.sopfc.domain.model.CoreExecutable;
import com.hyperbrain.sopfc.domain.port.in.MarkExecutableDoneUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executables")
public class CoreExecutableController {

    private final MarkExecutableDoneUseCase markExecutableDoneUseCase;

    public CoreExecutableController(MarkExecutableDoneUseCase markExecutableDoneUseCase) {
        this.markExecutableDoneUseCase = markExecutableDoneUseCase;
    }

    @PostMapping("/{executableId}/done")
    public ResponseEntity<CoreExecutableResponse> markAsDone(
            @PathVariable UUID executableId,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        CoreExecutable executable = markExecutableDoneUseCase.markAsDone(executableId, tenantId);
        
        CoreExecutableResponse response = new CoreExecutableResponse(
                executable.getId(),
                executable.getName(),
                executable.getStatus().name()
        );
        
        return ResponseEntity.ok(response);
    }

    // DTO interno para el Response
    public record CoreExecutableResponse(UUID id, String name, String status) {}
}