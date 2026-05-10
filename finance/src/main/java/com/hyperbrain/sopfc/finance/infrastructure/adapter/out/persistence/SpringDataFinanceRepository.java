package com.hyperbrain.sopfc.finance.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.finance.infrastructure.adapter.out.persistence.entity.FinanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SpringDataFinanceRepository extends JpaRepository<FinanceTransactionEntity, UUID> {
}
