package com.hyperbrain.sopfc.finance.domain.model;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class Transaction {
    UUID id;
    String description;
    BigDecimal amount;
    String type; // INCOME, EXPENSE
    UUID costCenterId;
}
