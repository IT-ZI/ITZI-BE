package com.itzi.itzi.store.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.store.dto.request.StoreRequestDTO;
import com.itzi.itzi.store.dto.response.StoreResponseDTO;
import com.itzi.itzi.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/profile")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    // 단건 조회
    @GetMapping("/store")
    public ResponseEntity<ApiResponse<StoreResponseDTO>> getStore(@PathVariable Long userId) {
        StoreResponseDTO store = storeService.getStoreByUserId(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus._OK, store));
    }

    // 부분 수정
    @PatchMapping("/store")
    public ResponseEntity<ApiResponse<StoreResponseDTO>> patchStore(
            @PathVariable Long userId,
            @Valid @RequestBody StoreRequestDTO dto
    ) {
        StoreResponseDTO updated = storeService.patchStoreByUserId(userId, dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PROFILE_UPDATED, updated));
    }
}
