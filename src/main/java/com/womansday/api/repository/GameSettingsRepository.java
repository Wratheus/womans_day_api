package com.womansday.api.repository;

import com.womansday.api.entity.GameSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSettingsRepository extends JpaRepository<GameSettings, Long> {
}
