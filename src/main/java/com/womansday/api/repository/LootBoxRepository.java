package com.womansday.api.repository;

import com.womansday.api.entity.LootBox;
import com.womansday.api.enums.LootBoxSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LootBoxRepository extends JpaRepository<LootBox, Long> {

    List<LootBox> findByUserIdOrderByCreatedAtEpochDesc(Long userId);

    @Query("SELECT COUNT(lb) FROM LootBox lb WHERE lb.user.id = :userId AND lb.prizeAmount IS NULL")
    int countUnopenedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(lb) FROM LootBox lb WHERE lb.user.id = :userId AND lb.prizeAmount IS NOT NULL")
    int countOpenedByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndSource(Long userId, LootBoxSource source);

    boolean existsByUserIdAndSource(Long userId, LootBoxSource source);

    @Query("SELECT lb FROM LootBox lb WHERE lb.source IS NULL")
    List<LootBox> findAllWithNullSource();

    @Query("SELECT COUNT(lb) FROM LootBox lb WHERE lb.prizeAmount IS NOT NULL")
    long countOpened();

    @Query("SELECT COUNT(lb) FROM LootBox lb WHERE lb.prizeAmount IS NULL")
    long countUnopened();

    @Query("SELECT COALESCE(SUM(lb.prizeAmount), 0) FROM LootBox lb WHERE lb.prizeAmount IS NOT NULL")
    long sumPrizeAmount();

    @Query("SELECT lb.prizeAmount, COUNT(lb) FROM LootBox lb WHERE lb.prizeAmount IS NOT NULL GROUP BY lb.prizeAmount")
    List<Object[]> countGroupedByPrizeAmount();

    @Query("SELECT lb.user.id, COUNT(lb), " +
            "SUM(CASE WHEN lb.prizeAmount IS NOT NULL THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN lb.prizeAmount IS NULL THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(lb.prizeAmount), 0) " +
            "FROM LootBox lb GROUP BY lb.user.id ORDER BY COALESCE(SUM(lb.prizeAmount), 0) DESC")
    List<Object[]> statsGroupedByUser();
}
