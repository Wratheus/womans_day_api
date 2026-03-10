package com.womansday.api.service;

import com.womansday.api.entity.GameSettings;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.repository.GameSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStateService {

    private static final Long SETTINGS_ID = 1L;

    private final GameSettingsRepository gameSettingsRepository;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        GameSettings settings = gameSettingsRepository.findById(SETTINGS_ID).orElse(null);
        if (settings == null) {
            settings = GameSettings.builder().id(SETTINGS_ID).gameFinished(false).build();
            gameSettingsRepository.save(settings);
        }
        finished.set(Boolean.TRUE.equals(settings.getGameFinished()));
        log.info("Game state loaded: finished={}", finished.get());
    }

    public boolean isFinished() {
        return finished.get();
    }

    @Transactional
    public boolean finishGame() {
        finished.set(true);
        GameSettings settings = gameSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        settings.setGameFinished(true);
        gameSettingsRepository.save(settings);
        log.info("Game finished by admin");
        return true;
    }

    @Transactional
    public boolean resumeGame() {
        finished.set(false);
        GameSettings settings = gameSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        settings.setGameFinished(false);
        gameSettingsRepository.save(settings);
        log.info("Game resumed by admin");
        return false;
    }

    public void requireGameActive() {
        if (finished.get()) {
            throw new BusinessLogicException("Игра завершена. Действия пользователей заблокированы.");
        }
    }
}
