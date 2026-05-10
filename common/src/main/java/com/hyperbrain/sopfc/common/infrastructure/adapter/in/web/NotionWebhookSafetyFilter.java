package com.hyperbrain.sopfc.common.infrastructure.adapter.in.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extreme safety filter for Notion Webhooks.
 * Intercepts the response to ensure it's ALWAYS 202 Accepted for Notion paths,
 * preventing Notion from blocking the integration due to any internal error (4xx or 5xx).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotionWebhookSafetyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        if (path != null && (path.contains("/api/v1/sync/notion") || path.contains("/api/notion/webhook"))) {
            log.debug("🛡️ [NOTION-FILTER] Protecting path: {}", path);
            
            // Wrap response to catch status changes
            StatusCapturingResponseWrapper wrappedResponse = new StatusCapturingResponseWrapper(httpResponse);
            
            try {
                chain.doFilter(request, wrappedResponse);
            } catch (Exception e) {
                log.error("🚨 [NOTION-FILTER] Critical failure in chain for {}: {}", path, e.getMessage());
            } finally {
                // FORCE 202 Accepted no matter what happened in the chain
                if (httpResponse.getStatus() != HttpServletResponse.SC_ACCEPTED) {
                    log.info("⚠️ [NOTION-FILTER] Overriding status {} with 202 for Notion path", wrappedResponse.getStatus());
                    httpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {
        private int status = SC_OK;

        public StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public int getStatus() {
            return status;
        }
    }
}
