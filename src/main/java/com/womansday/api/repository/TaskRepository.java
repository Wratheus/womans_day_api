package com.womansday.api.repository;

import com.womansday.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT COALESCE(SUM(t.reward), 0) FROM Task t")
    long sumAllRewards();
}
