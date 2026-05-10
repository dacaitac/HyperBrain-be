package com.hyperbrain.sopfc.finance.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FinTransactionTest {

    @Test
    void testCreateValidTransaction() {
        UUID executableId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        FinTransaction transaction = new FinTransaction(
                UUID.randomUUID(),
                executableId,
                amount,
                "USD",
                "Gasto en SaaS para desarrollo",
                FinTransaction.TransactionType.EXPENSE,
                OffsetDateTime.now()
        );

        assertEquals(amount, transaction.amount());
        assertEquals(FinTransaction.TransactionType.EXPENSE, transaction.type());
        assertEquals(executableId, transaction.executableId());
    }
}
