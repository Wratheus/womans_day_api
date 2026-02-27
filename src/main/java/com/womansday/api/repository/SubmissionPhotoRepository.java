package com.womansday.api.repository;

import com.womansday.api.entity.SubmissionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubmissionPhotoRepository extends JpaRepository<SubmissionPhoto, Long> {

    @Query("SELECT sp FROM SubmissionPhoto sp JOIN sp.submission s JOIN s.participants p " +
            "WHERE sp.id = :photoId AND p.id = :userId")
    Optional<SubmissionPhoto> findByIdAndParticipant(@Param("photoId") Long photoId,
                                                      @Param("userId") Long userId);
}
