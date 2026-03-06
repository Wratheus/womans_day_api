package com.womansday.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loot_boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Legacy column — kept to satisfy NOT NULL DB constraint; always 0 for new boxes
    @Builder.Default
    @Column(nullable = false)
    private Integer cost = 0;

    @Column(name = "prize_amount")
    private Integer prizeAmount;

    @Column(name = "opened_at_epoch")
    private Long openedAtEpoch;

    @Column(name = "created_at_epoch", nullable = false)
    private Long createdAtEpoch;

    @PrePersist
    protected void onCreate() {
        if (createdAtEpoch == null) {
            createdAtEpoch = java.time.Instant.now().toEpochMilli();
        }
    }
}
