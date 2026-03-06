package com.womansday.api.service;

import com.womansday.api.dto.response.BalanceHistoryEntry;
import com.womansday.api.dto.response.LootBoxResponse;
import com.womansday.api.entity.BalanceTransaction;
import com.womansday.api.entity.LootBox;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.TransactionType;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.BalanceTransactionRepository;
import com.womansday.api.repository.LootBoxRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LootBoxService {

    private static final int LOOTBOX_MILESTONE = 150;

    // {amount, weight} — weights sum to 100, EV ≈ 23.5
    private static final int[][] PRIZE_TIERS = {
            {5,   50},
            {10,  25},
            {30,  15},
            {80,   7},
            {280,  3},
    };

    private static final int TOTAL_WEIGHT;

    static {
        int w = 0;
        for (int[] tier : PRIZE_TIERS) {
            w += tier[1];
        }
        TOTAL_WEIGHT = w;
    }

    private final LootBoxRepository lootBoxRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final UserRepository userRepository;

    public void checkAndAwardFirstTaskBonus(User user) {
        long taskRewardCount = balanceTransactionRepository.countByUserIdAndType(user.getId(), TransactionType.TASK_REWARD);
        if (taskRewardCount == 1) {
            lootBoxRepository.save(LootBox.builder().user(user).build());
            log.info("First-task bonus lootbox awarded: userId={}", user.getId());
        }
    }

    @Transactional
    public int giftFirstTaskBonusToEligibleUsers() {
        List<User> users = userRepository.findByRoleNot(Role.ADMIN);

        List<LootBox> boxes = new ArrayList<>();
        for (User user : users) {
            boolean hasTask = balanceTransactionRepository.countByUserIdAndType(
                    user.getId(), TransactionType.TASK_REWARD) >= 1;
            boolean hasBox = lootBoxRepository.countByUserId(user.getId()) >= 1;

            if (hasTask && !hasBox) {
                boxes.add(LootBox.builder().user(user).build());
            }
        }

        if (!boxes.isEmpty()) {
            lootBoxRepository.saveAll(boxes);
            log.info("First-task bonus migration: {} lootboxes issued", boxes.size());
        }
        return boxes.size();
    }

    public void checkAndAwardMilestoneBoxes(User user) {
        long taskEarnings = balanceTransactionRepository.sumByUserIdAndType(user.getId(), TransactionType.TASK_REWARD);
        long boxesEarned = taskEarnings / LOOTBOX_MILESTONE;
        long existingBoxes = lootBoxRepository.countByUserId(user.getId());
        long toAward = boxesEarned - existingBoxes;

        if (toAward > 0) {
            List<LootBox> boxes = new ArrayList<>();
            for (int i = 0; i < toAward; i++) {
                boxes.add(LootBox.builder().user(user).build());
            }
            lootBoxRepository.saveAll(boxes);
            log.info("Milestone lootboxes awarded: userId={}, count={}", user.getId(), toAward);
        }
    }

    @Transactional
    public LootBoxResponse open(Long lootBoxId, Long userId) {
        LootBox lootBox = lootBoxRepository.findById(lootBoxId)
                .orElseThrow(() -> new ResourceNotFoundException("Лутбокс не найден"));

        if (!lootBox.getUser().getId().equals(userId)) {
            throw new BusinessLogicException("Это не ваш лутбокс");
        }

        if (lootBox.getPrizeAmount() != null) {
            throw new BusinessLogicException("Лутбокс уже открыт");
        }

        int prizeAmount = rollPrize();
        lootBox.setPrizeAmount(prizeAmount);
        lootBox.setOpenedAtEpoch(java.time.Instant.now().toEpochMilli());
        lootBoxRepository.save(lootBox);

        balanceTransactionRepository.save(BalanceTransaction.builder()
                .user(lootBox.getUser())
                .type(TransactionType.LOOTBOX_PRIZE)
                .amount(prizeAmount)
                .referenceId(lootBox.getId())
                .description("Выигрыш: " + prizeAmount + " тюльпанов")
                .build());

        log.info("LootBox opened: id={}, userId={}, prize={}", lootBoxId, userId, prizeAmount);
        return toLootBoxResponse(lootBox);
    }

    @Transactional(readOnly = true)
    public List<LootBoxResponse> getMyLootBoxes(Long userId) {
        return lootBoxRepository.findByUserIdOrderByCreatedAtEpochDesc(userId).stream()
                .map(this::toLootBoxResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanceHistoryEntry> getHistory(Long userId) {
        return balanceTransactionRepository.findByUserIdOrderByCreatedAtEpochDesc(userId).stream()
                .map(bt -> BalanceHistoryEntry.builder()
                        .id(bt.getId())
                        .type(bt.getType())
                        .amount(bt.getAmount())
                        .description(bt.getDescription())
                        .createdAtEpoch(bt.getCreatedAtEpoch())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public int giftToAll() {
        List<User> users = userRepository.findByRoleNot(Role.ADMIN);

        List<LootBox> boxes = users.stream()
                .map(user -> LootBox.builder().user(user).build())
                .collect(Collectors.toList());

        lootBoxRepository.saveAll(boxes);

        log.info("Gift lootboxes issued to {} users", boxes.size());
        return boxes.size();
    }

    private int rollPrize() {
        int roll = ThreadLocalRandom.current().nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (int[] tier : PRIZE_TIERS) {
            cumulative += tier[1];
            if (roll < cumulative) {
                return tier[0];
            }
        }
        return PRIZE_TIERS[PRIZE_TIERS.length - 1][0];
    }

    private LootBoxResponse toLootBoxResponse(LootBox lb) {
        return LootBoxResponse.builder()
                .id(lb.getId())
                .prizeAmount(lb.getPrizeAmount())
                .openedAtEpoch(lb.getOpenedAtEpoch())
                .createdAtEpoch(lb.getCreatedAtEpoch())
                .build();
    }
}
