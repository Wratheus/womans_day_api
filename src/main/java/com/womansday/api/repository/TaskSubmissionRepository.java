package com.womansday.api.repository;

import com.womansday.api.entity.TaskSubmission;
import com.womansday.api.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    boolean existsByUserIdAndTaskId(Long userId, Long taskId);

    Optional<TaskSubmission> findByUserIdAndTaskId(Long userId, Long taskId);

    List<TaskSubmission> findByTaskId(Long taskId);

    @Query("SELECT COALESCE(SUM(ts.task.reward), 0) FROM TaskSubmission ts WHERE ts.status = :status")
    long sumRewardsByStatus(@Param("status") SubmissionStatus status);
}
