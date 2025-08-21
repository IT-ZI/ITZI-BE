package com.itzi.itzi.partnership.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.partnership.dto.request.PartnershipPatchRequestDTO;
import com.itzi.itzi.partnership.dto.request.PartnershipPostRequestDTO;
import com.itzi.itzi.partnership.dto.response.PartnershipPatchResponseDTO;
import com.itzi.itzi.partnership.dto.response.PartnershipPostResponseDTO;
import com.itzi.itzi.partnership.service.PartnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/partnership")
@RequiredArgsConstructor
@Slf4j
public class PartnershipController {
    private final PartnershipService partnershipService;

    // "AI 문의 글 변환" 버튼을 눌렀을 때 사용자가 입력한 값을 저장
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<PartnershipPostResponseDTO>> postInquiry(
            @PathVariable Long userId,
            @Valid @RequestBody PartnershipPostRequestDTO dto) {
        PartnershipPostResponseDTO inquiry =
                partnershipService.postInquiry(userId, dto.getReceiverId(), dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_POST, inquiry));
    }


    // AI 문의 글이 완성되고 수정한 뒤 "보내기" 버튼을 누르면, status=send / content 값 저장 --> 일부만 수정되므로 Patch 사용
    @PatchMapping("/{partnershipId}/send")
    public ResponseEntity<ApiResponse<PartnershipPatchResponseDTO>> sendInquiry(
            @PathVariable Long partnershipId,
            @Valid @RequestBody PartnershipPatchRequestDTO dto) {
        PartnershipPatchResponseDTO inquiry =
                partnershipService.patchInquiry(partnershipId, dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_SENT, inquiry));
    }
}
