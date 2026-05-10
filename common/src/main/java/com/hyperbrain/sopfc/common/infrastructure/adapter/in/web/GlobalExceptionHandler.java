package com.hyperbrain.sopfc.common.infrastructure.adapter.in.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Safety net for Notion Webhooks. 
     * If anything fails anywhere in the request lifecycle for a Notion sync path,
     * we return 202 Accepted to prevent Notion from disabling the webhook.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleAllExceptions(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        if (uri != null && uri.contains("/api/v1/sync/notion")) {
            log.error("🚨 [GLOBAL-EX-HANDLER] Intercepted error in Notion Webhook path ({}): {}", uri, ex.getMessage(), ex);
            return ResponseEntity.accepted().build();
        }
        
        // For other paths, we might want to return standard 500, but for now, 
        // to be extra safe during this E2E stabilization phase, we log it.
        log.error("🚨 [GLOBAL-EX-HANDLER] Unhandled exception in {}: {}", uri, ex.getMessage(), ex);
        return ResponseEntity.internalServerError().build();
    }
}
