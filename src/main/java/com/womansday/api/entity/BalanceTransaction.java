package com.womansday.api.entity;

import com.womansday.api.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "balance_transactions", indexes = {
    @Index(name = "idx_bt_user_id", columnList = "user_id"),
    @Index(name = "idx_bt_type_ref", columnList = "type, reference_id"),
    @Index(name = "idx_bt_type_ref_user", columnList = "type, reference_id, user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 300)
    private String description;

    @Column(name = "created_at_epoch", nullable = false)
    private Long createdAtEpoch;

    @PrePersist
    protected void onCreate() {
        if (createdAtEpoch == null) {
            createdAtEpoch = java.time.Instant.now().toEpochMilli();
        }
    }
}
