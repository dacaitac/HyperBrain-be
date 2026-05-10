package com.hyperbrain.sopfc.common.infrastructure.config;

/**
 * Holder to track the source of a change in the current thread/transaction.
 * Helps prevent sync loops (echo-sync).
 */
public class SyncContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    public static final String INTERNAL_SOURCE = "SOPFC_INTERNAL";

    public static void setSource(String source) {
        CONTEXT.set(source);
    }

    public static String getSource() {
        String source = CONTEXT.get();
        return source != null ? source : INTERNAL_SOURCE;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
