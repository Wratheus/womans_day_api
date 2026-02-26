package com.womansday.api.repository;

import com.womansday.api.entity.TaskSubmission;
import com.womansday.api.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    List<TaskSubmission> findByTaskId(Long taskId);

    List<TaskSubmission> findByStatusOrderByCreatedAtAsc(SubmissionStatus status);

    @Query("SELECT s FROM TaskSubmission s ORDER BY " +
            "CASE s.status WHEN 'PENDING' THEN 0 WHEN 'REJECTED' THEN 1 WHEN 'APPROVED' THEN 2 END, " +
            "s.createdAt ASC")
    List<TaskSubmission> findAllOrderByStatusAndCreatedAt();

    @Query("SELECT s FROM TaskSubmission s JOIN s.participants p WHERE p.id = :userId AND s.task.id = :taskId ORDER BY s.createdAt DESC")
    List<TaskSubmission> findByParticipantAndTaskId(@Param("userId") Long userId, @Param("taskId") Long taskId);

    @Query("SELECT COUNT(s) > 0 FROM TaskSubmission s JOIN s.participants p " +
            "WHERE p.id = :userId AND s.task.id = :taskId AND s.status IN ('PENDING', 'APPROVED')")
    boolean hasActiveSubmission(@Param("userId") Long userId, @Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(s.task.reward), 0) FROM TaskSubmission s JOIN s.participants p WHERE s.status = :status")
    long sumRewardsByStatus(@Param("status") SubmissionStatus status);
}
