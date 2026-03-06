package com.womansday.api.service;

import com.womansday.api.dto.response.BalanceHistoryEntry;
import com.womansday.api.dto.response.LootBoxResponse;
import com.womansday.api.entity.BalanceTransaction;
import com.womansday.api.entity.LootBox;
import com.womansday.api.entity.User;
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LootBoxService {

    private static final int LOOTBOX_COST = 30;

    private static final int[][] PRIZE_TIERS = {
            // {amount, weight}
            {5, 30},
            {10, 25},
            {20, 20},
            {30, 15},
            {50, 8},
            {100, 2},
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
    public LootBoxResponse purchase(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        long balance = balanceTransactionRepository.sumByUserId(userId);
        if (balance < LOOTBOX_COST) {
            throw new BusinessLogicException("Недостаточно тюльпанов для покупки лутбокса");
        }

        LootBox lootBox = LootBox.builder()
                .user(user)
                .cost(LOOTBOX_COST)
                .build();
        lootBox = lootBoxRepository.save(lootBox);

        balanceTransactionRepository.save(BalanceTransaction.builder()
                .user(user)
                .type(TransactionType.LOOTBOX_PURCHASE)
                .amount(-LOOTBOX_COST)
                .referenceId(lootBox.getId())
                .description("Покупка лутбокса")
                .build());

        log.info("LootBox purchased: id={}, userId={}, cost={}", lootBox.getId(), userId, LOOTBOX_COST);
        return toLootBoxResponse(lootBox);
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

    public int getLootBoxCost() {
        return LOOTBOX_COST;
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
                .cost(lb.getCost())
                .prizeAmount(lb.getPrizeAmount())
                .openedAtEpoch(lb.getOpenedAtEpoch())
                .createdAtEpoch(lb.getCreatedAtEpoch())
                .build();
    }
}
