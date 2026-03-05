package com.womansday.api.repository;

import com.womansday.api.entity.SubmissionMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubmissionMediaRepository extends JpaRepository<SubmissionMedia, Long> {

    @Query("SELECT sm FROM SubmissionMedia sm JOIN sm.submission s JOIN s.participants p " +
            "WHERE sm.id = :mediaId AND p.id = :userId")
    Optional<SubmissionMedia> findByIdAndParticipant(@Param("mediaId") Long mediaId,
                                                      @Param("userId") Long userId);
}
