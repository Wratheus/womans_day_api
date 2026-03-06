package com.womansday.api.controller;

import com.womansday.api.dto.response.LootBoxResponse;
import com.womansday.api.security.SecurityUtils;
import com.womansday.api.service.LootBoxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lootbox")
@RequiredArgsConstructor
public class LootBoxController {

    private final LootBoxService lootBoxService;

    @GetMapping
    public ResponseEntity<List<LootBoxResponse>> getMyLootBoxes(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.ok(lootBoxService.getMyLootBoxes(userId));
    }

    @PostMapping("/{id}/open")
    public ResponseEntity<LootBoxResponse> open(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.ok(lootBoxService.open(id, userId));
    }
}
