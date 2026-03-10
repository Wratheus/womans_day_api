package com.womansday.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSettings {

    @Id
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private Boolean gameFinished = false;
}
