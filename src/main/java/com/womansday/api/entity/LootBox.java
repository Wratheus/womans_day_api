package com.womansday.api.entity;

import com.womansday.api.enums.LootBoxSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loot_boxes", indexes = {
    @Index(name = "idx_lb_user_id", columnList = "user_id"),
    @Index(name = "idx_lb_user_source", columnList = "user_id, source")
})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private LootBoxSource source;

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
