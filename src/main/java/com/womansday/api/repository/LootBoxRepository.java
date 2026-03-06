package com.womansday.api.repository;

import com.womansday.api.entity.LootBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LootBoxRepository extends JpaRepository<LootBox, Long> {

    List<LootBox> findByUserIdOrderByCreatedAtEpochDesc(Long userId);

    @Query("SELECT COUNT(lb) FROM LootBox lb WHERE lb.user.id = :userId AND lb.prizeAmount IS NULL")
    int countUnopenedByUserId(@Param("userId") Long userId);
}
