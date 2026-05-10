package com.hyperbrain.sopfc.finance.domain.port.in;

import com.hyperbrain.sopfc.finance.domain.model.Transaction;
import java.util.UUID;

public interface RegisterTransactionUseCase {
    Transaction register(Transaction transaction);
}
