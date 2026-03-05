package com.womansday.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submission_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private TaskSubmission submission;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String contentType;
}
