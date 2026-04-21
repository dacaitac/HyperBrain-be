package com.hyperbrain.sopfc.finance.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa una transacción financiera (Micro-Gasto o Micro-Ingreso).
 * Atribuye un costo económico a la ejecución de una tarea (ROI).
 */
public record FinTransaction(
    UUID id,
    UUID tenantId,
    UUID executableId,
    BigDecimal amount,
    String currency,
    String description,
    TransactionType type,
    OffsetDateTime occurredAt
) {
    public enum TransactionType {
        INCOME, EXPENSE
    }
}
