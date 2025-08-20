package com.itzi.itzi.auth.controller;

import com.itzi.itzi.auth.dto.request.UserOrgProfileRequestDTO;
import com.itzi.itzi.auth.dto.response.UserOrgProfileResponseDTO;
import com.itzi.itzi.auth.service.UserService;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.store.dto.response.StoreResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.itzi.itzi.global.api.code.SuccessStatus;

@RestController
@RequestMapping("/users/{userId}/profile")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    // User & OrgProfile 조회
    @GetMapping
    public ResponseEntity<ApiResponse<UserOrgProfileResponseDTO>> getProfile(@PathVariable Long userId) {
        UserOrgProfileResponseDTO userOrgProfile = userService.getUserAndOrgProfile(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus._OK, userOrgProfile));
    }

    // 프로필 수정
    @PatchMapping
    public ResponseEntity<ApiResponse<UserOrgProfileResponseDTO>> patchProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserOrgProfileRequestDTO dto
    ) {
        UserOrgProfileResponseDTO updated = userService.patchUserAndOrgProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PROFILE_UPDATED, updated));
    }

}
