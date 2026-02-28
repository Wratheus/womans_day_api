package com.womansday.api.entity;

import com.womansday.api.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer reward;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskType type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean collaborative = false;
}
