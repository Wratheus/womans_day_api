package com.womansday.api.repository;

import com.womansday.api.entity.SubmissionMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionMediaRepository extends JpaRepository<SubmissionMedia, Long> {

    @Query("SELECT sm FROM SubmissionMedia sm JOIN sm.submission s JOIN s.participants p " +
            "WHERE sm.id = :mediaId AND p.id = :userId")
    Optional<SubmissionMedia> findByIdAndParticipant(@Param("mediaId") Long mediaId,
                                                      @Param("userId") Long userId);

    @Query("""
            SELECT sm FROM SubmissionMedia sm
            JOIN FETCH sm.submission s
            JOIN FETCH s.task t
            JOIN FETCH s.submitter u
            WHERE s.status = 'APPROVED'
            ORDER BY t.id, s.id, sm.id
            """)
    List<SubmissionMedia> findAllApprovedWithDetails();
}
