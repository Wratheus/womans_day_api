package com.womansday.api.repository;

import com.womansday.api.entity.TaskSubmission;
import com.womansday.api.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

        List<TaskSubmission> findByStatusOrderByCreatedAtEpochAsc(SubmissionStatus status);

        @Query("SELECT s FROM TaskSubmission s ORDER BY " +
                        "CASE s.status WHEN 'PENDING' THEN 0 WHEN 'WAITING_FOR_PARTICIPANTS' THEN 1 WHEN 'REJECTED' THEN 2 WHEN 'APPROVED' THEN 3 WHEN 'CANCELLED' THEN 4 END, "
                        +
                        "s.createdAtEpoch ASC")
        List<TaskSubmission> findAllOrderByStatusAndCreatedAtEpoch();

        @Query("SELECT s FROM TaskSubmission s JOIN s.participants p WHERE p.id = :userId AND s.task.id = :taskId ORDER BY s.createdAtEpoch DESC")
        List<TaskSubmission> findByParticipantAndTaskId(@Param("userId") Long userId, @Param("taskId") Long taskId);

        @Query("SELECT s FROM TaskSubmission s JOIN s.pendingParticipants pp WHERE pp.id = :userId AND s.task.id = :taskId ORDER BY s.createdAtEpoch DESC")
        List<TaskSubmission> findByPendingParticipantAndTaskId(@Param("userId") Long userId,
                        @Param("taskId") Long taskId);

        @Query("""
                            SELECT COUNT(s) > 0
                            FROM TaskSubmission s
                            JOIN s.pendingParticipants pp
                            WHERE pp.id = :userId
                              AND s.task.id = :taskId
                              AND s.status = 'WAITING_FOR_PARTICIPANTS'
                              AND s.id <> :excludeSubmissionId
                        """)
        boolean hasPendingInvitationOtherThan(@Param("userId") Long userId,
                        @Param("taskId") Long taskId,
                        @Param("excludeSubmissionId") Long excludeSubmissionId);

        @Query("SELECT COUNT(s) > 0 FROM TaskSubmission s JOIN s.participants p " +
                        "WHERE p.id = :userId AND s.task.id = :taskId AND s.status IN ('PENDING', 'APPROVED', 'WAITING_FOR_PARTICIPANTS')")
        boolean hasActiveSubmission(@Param("userId") Long userId, @Param("taskId") Long taskId);

        @Query("SELECT COUNT(s) > 0 FROM TaskSubmission s JOIN s.pendingParticipants pp " +
                        "WHERE pp.id = :userId AND s.task.id = :taskId AND s.status = 'WAITING_FOR_PARTICIPANTS'")
        boolean hasPendingInvitation(@Param("userId") Long userId, @Param("taskId") Long taskId);

        @Query("SELECT COUNT(s) > 0 FROM TaskSubmission s " +
                        "WHERE s.task.id = :taskId AND s.status IN ('PENDING', 'APPROVED', 'WAITING_FOR_PARTICIPANTS')")
        boolean hasActiveSubmissionsForTask(@Param("taskId") Long taskId);

        @Query("SELECT COALESCE(SUM(s.task.reward), 0) FROM TaskSubmission s JOIN s.participants p WHERE s.status = :status")
        long sumRewardsByStatus(@Param("status") SubmissionStatus status);

        @Query("SELECT COALESCE(SUM(s.task.reward), 0) FROM TaskSubmission s JOIN s.participants p " +
                        "WHERE p.id = :userId AND s.status = 'APPROVED'")
        long sumApprovedRewardsByUserId(@Param("userId") Long userId);

        @Query("SELECT p.id, COALESCE(SUM(s.task.reward), 0) FROM TaskSubmission s JOIN s.participants p " +
                        "WHERE s.status = 'APPROVED' GROUP BY p.id")
        List<Object[]> sumApprovedRewardsGroupedByUser();

        @Query("SELECT s FROM TaskSubmission s JOIN s.participants p WHERE p.id = :userId ORDER BY s.createdAtEpoch DESC")
        List<TaskSubmission> findByParticipantId(@Param("userId") Long userId);

        @Query("SELECT s FROM TaskSubmission s JOIN s.pendingParticipants pp WHERE pp.id = :userId AND s.status = 'WAITING_FOR_PARTICIPANTS' ORDER BY s.createdAtEpoch DESC")
        List<TaskSubmission> findPendingInvitationsByUserId(@Param("userId") Long userId);

        @Query("""
                            select distinct s
                            from TaskSubmission s
                            join s.participants me
                            join fetch s.task t
                            left join fetch s.participants p
                            where me.id = :userId
                              and s.status = :status
                        """)
        List<TaskSubmission> findByParticipantAndStatusWithTaskAndParticipants(
                        @Param("userId") Long userId,
                        @Param("status") SubmissionStatus status);

        @Query("""
                            select distinct s
                            from TaskSubmission s
                            join fetch s.task t
                            join fetch s.participants p
                            join fetch s.submitter
                            where s.status = 'APPROVED'
                        """)
        List<TaskSubmission> findAllApprovedWithParticipants();
}
