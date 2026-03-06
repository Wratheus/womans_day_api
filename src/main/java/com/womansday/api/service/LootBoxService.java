package com.womansday.api.service;

import com.womansday.api.dto.response.BalanceHistoryEntry;
import com.womansday.api.dto.response.LootBoxResponse;
import com.womansday.api.entity.BalanceTransaction;
import com.womansday.api.entity.LootBox;
import com.womansday.api.entity.User;
import com.womansday.api.enums.LootBoxSource;
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.womansday.api.dto.response.LootBoxStatsResponse;
import com.womansday.api.dto.response.UserBalanceStatsResponse;

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

    @Transactional
    public int migrateNullSourceLootBoxes() {
        List<LootBox> nullBoxes = lootBoxRepository.findAllWithNullSource();
        if (nullBoxes.isEmpty()) {
            return 0;
        }

        // Группируем по юзеру, сортируем по дате создания
        Map<Long, List<LootBox>> byUser = new HashMap<>();
        for (LootBox box : nullBoxes) {
            byUser.computeIfAbsent(box.getUser().getId(), k -> new ArrayList<>()).add(box);
        }

        for (List<LootBox> userBoxes : byUser.values()) {
            userBoxes.sort(Comparator.comparingLong(LootBox::getCreatedAtEpoch));
            // Самый старый — FIRST_TASK, остальные — MILESTONE
            userBoxes.get(0).setSource(LootBoxSource.FIRST_TASK);
            for (int i = 1; i < userBoxes.size(); i++) {
                userBoxes.get(i).setSource(LootBoxSource.MILESTONE);
            }
        }

        lootBoxRepository.saveAll(nullBoxes);
        log.info("Migrated {} lootboxes with null source (FIRST_TASK + MILESTONE)", nullBoxes.size());
        return nullBoxes.size();
    }

    public void checkAndAwardFirstTaskBonus(User user) {
        if (lootBoxRepository.existsByUserIdAndSource(user.getId(), LootBoxSource.FIRST_TASK)) {
            return;
        }
        long taskRewardCount = balanceTransactionRepository.countByUserIdAndType(user.getId(), TransactionType.TASK_REWARD);
        if (taskRewardCount >= 1) {
            lootBoxRepository.save(LootBox.builder().user(user).source(LootBoxSource.FIRST_TASK).build());
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
            boolean alreadyHas = lootBoxRepository.existsByUserIdAndSource(user.getId(), LootBoxSource.FIRST_TASK);

            if (hasTask && !alreadyHas) {
                boxes.add(LootBox.builder().user(user).source(LootBoxSource.FIRST_TASK).build());
            }
        }

        if (!boxes.isEmpty()) {
            lootBoxRepository.saveAll(boxes);
            log.info("First-task bonus migration: {} lootboxes issued", boxes.size());
        }
        return boxes.size();
    }

    private static final long MILESTONE_CAP = 2000;

    public void checkAndAwardMilestoneBoxes(User user) {
        long taskEarnings = balanceTransactionRepository.sumByUserIdAndType(user.getId(), TransactionType.TASK_REWARD);
        if (taskEarnings > MILESTONE_CAP) {
            taskEarnings = MILESTONE_CAP;
        }
        long boxesEarned = taskEarnings / LOOTBOX_MILESTONE;
        long existingMilestoneBoxes = lootBoxRepository.countByUserIdAndSource(user.getId(), LootBoxSource.MILESTONE);
        long toAward = boxesEarned - existingMilestoneBoxes;

        if (toAward > 0) {
            List<LootBox> boxes = new ArrayList<>();
            for (int i = 0; i < toAward; i++) {
                boxes.add(LootBox.builder().user(user).source(LootBoxSource.MILESTONE).build());
            }
            lootBoxRepository.saveAll(boxes);
            log.info("Milestone lootboxes awarded: userId={}, count={}", user.getId(), toAward);
        }
    }

    @Transactional
    public int giftMilestoneBoxesToEligibleUsers() {
        List<User> users = userRepository.findByRoleNot(Role.ADMIN);

        List<LootBox> boxes = new ArrayList<>();
        for (User user : users) {
            long taskEarnings = balanceTransactionRepository.sumByUserIdAndType(user.getId(), TransactionType.TASK_REWARD);
            if (taskEarnings > MILESTONE_CAP) {
                taskEarnings = MILESTONE_CAP;
            }
            long boxesEarned = taskEarnings / LOOTBOX_MILESTONE;
            long existingMilestoneBoxes = lootBoxRepository.countByUserIdAndSource(user.getId(), LootBoxSource.MILESTONE);
            long toAward = boxesEarned - existingMilestoneBoxes;

            for (int i = 0; i < toAward; i++) {
                boxes.add(LootBox.builder().user(user).source(LootBoxSource.MILESTONE).build());
            }
        }

        if (!boxes.isEmpty()) {
            lootBoxRepository.saveAll(boxes);
            log.info("Milestone bonus migration: {} lootboxes issued", boxes.size());
        }
        return boxes.size();
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
    public int giftToUser(Long userId, int count) {
        if (count < 1) {
            throw new BusinessLogicException("Количество должно быть >= 1");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<LootBox> boxes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boxes.add(LootBox.builder().user(user).source(LootBoxSource.GIFT).build());
        }
        lootBoxRepository.saveAll(boxes);

        log.info("Gift lootboxes issued: userId={}, count={}", userId, count);
        return count;
    }

    @Transactional
    public int giftToAll() {
        List<User> users = userRepository.findByRoleNot(Role.ADMIN);

        List<LootBox> boxes = users.stream()
                .map(user -> LootBox.builder().user(user).source(LootBoxSource.GIFT).build())
                .collect(Collectors.toList());

        lootBoxRepository.saveAll(boxes);

        log.info("Gift lootboxes issued to {} users", boxes.size());
        return boxes.size();
    }

    @Transactional(readOnly = true)
    public LootBoxStatsResponse getLootBoxStats() {
        long totalOpened = lootBoxRepository.countOpened();
        long totalUnopened = lootBoxRepository.countUnopened();
        long totalPrizeSum = lootBoxRepository.sumPrizeAmount();

        Map<Integer, Long> actualCounts = new HashMap<>();
        for (Object[] row : lootBoxRepository.countGroupedByPrizeAmount()) {
            actualCounts.put((Integer) row[0], (Long) row[1]);
        }

        List<LootBoxStatsResponse.TierStats> tiers = new ArrayList<>();
        for (int[] tier : PRIZE_TIERS) {
            int amount = tier[0];
            int weight = tier[1];
            long count = actualCounts.getOrDefault(amount, 0L);
            tiers.add(LootBoxStatsResponse.TierStats.builder()
                    .prizeAmount(amount)
                    .configuredWeight(weight)
                    .configuredChance(Math.round(weight * 10000.0 / TOTAL_WEIGHT) / 100.0)
                    .actualCount(count)
                    .actualChance(totalOpened > 0
                            ? Math.round(count * 10000.0 / totalOpened) / 100.0
                            : 0.0)
                    .build());
        }

        return LootBoxStatsResponse.builder()
                .totalOpened(totalOpened)
                .totalUnopened(totalUnopened)
                .totalPrizeSum(totalPrizeSum)
                .tiers(tiers)
                .build();
    }

    @Transactional(readOnly = true)
    public UserBalanceStatsResponse getUserBalanceStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        long currentBalance = balanceTransactionRepository.sumByUserId(userId);

        Map<String, Long> totalByType = new LinkedHashMap<>();
        for (TransactionType type : TransactionType.values()) {
            long sum = balanceTransactionRepository.sumByUserIdAndType(userId, type);
            totalByType.put(type.value(), sum);
        }

        List<BalanceHistoryEntry> history = balanceTransactionRepository
                .findByUserIdOrderByCreatedAtEpochDesc(userId).stream()
                .map(bt -> BalanceHistoryEntry.builder()
                        .id(bt.getId())
                        .type(bt.getType())
                        .amount(bt.getAmount())
                        .description(bt.getDescription())
                        .createdAtEpoch(bt.getCreatedAtEpoch())
                        .build())
                .collect(Collectors.toList());

        return UserBalanceStatsResponse.builder()
                .userId(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .currentBalance(currentBalance)
                .totalByType(totalByType)
                .history(history)
                .build();
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
