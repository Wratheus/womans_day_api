package com.womansday.api.repository;

import com.womansday.api.entity.BalanceTransaction;
import com.womansday.api.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, Long> {

    @Query("SELECT COALESCE(SUM(bt.amount), 0) FROM BalanceTransaction bt WHERE bt.user.id = :userId")
    long sumByUserId(@Param("userId") Long userId);

    @Query("SELECT bt.user.id, COALESCE(SUM(bt.amount), 0) FROM BalanceTransaction bt GROUP BY bt.user.id")
    List<Object[]> sumGroupedByUser();

    List<BalanceTransaction> findByUserIdOrderByCreatedAtEpochDesc(Long userId);

    boolean existsByTypeAndReferenceId(TransactionType type, Long referenceId);

    @Query("SELECT COUNT(bt) > 0 FROM BalanceTransaction bt WHERE bt.type = :type AND bt.referenceId = :refId AND bt.user.id = :userId")
    boolean existsByTypeAndReferenceIdAndUserId(@Param("type") TransactionType type,
            @Param("refId") Long referenceId, @Param("userId") Long userId);
}
