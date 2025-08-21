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

import java.util.List;

@RestController
@RequestMapping("/partnership")
@RequiredArgsConstructor
@Slf4j
public class PartnershipController {
    private final PartnershipService partnershipService;

    /** 1. AI 문의 글 변환 (초안 저장: DRAFT) */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<PartnershipPostResponseDTO>> postInquiry(
            @PathVariable Long userId,
            @Valid @RequestBody PartnershipPostRequestDTO dto) {
        PartnershipPostResponseDTO inquiry =
                partnershipService.postInquiry(userId, dto.getReceiverId(), dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_POST, inquiry));
    }

    /** 2. AI 문의 글 완성 후 보내기 (PATCH → SEND) */
    @PatchMapping("/{partnershipId}/send")
    public ResponseEntity<ApiResponse<PartnershipPatchResponseDTO>> sendInquiry(
            @PathVariable Long partnershipId,
            @RequestBody(required = false) PartnershipPatchRequestDTO dto) {
        PartnershipPatchResponseDTO inquiry =
                partnershipService.patchInquiry(partnershipId, dto);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_SENT, inquiry));
    }

    /** 3. 내가 보낸 문의 조회 */
    @GetMapping("/{userId}/sent")
    public ResponseEntity<ApiResponse<List<PartnershipPostResponseDTO>>> getSentInquiries(
            @PathVariable Long userId) {
        List<PartnershipPostResponseDTO> list = partnershipService.getSentInquiries(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus._OK, list));
    }

    /** 4. 내가 받은 문의 조회 */
    @GetMapping("/{userId}/received")
    public ResponseEntity<ApiResponse<List<PartnershipPostResponseDTO>>> getReceivedInquiries(
            @PathVariable Long userId) {
        List<PartnershipPostResponseDTO> list = partnershipService.getReceivedInquiries(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus._OK, list));
    }

    /** 5. 받은 문의 수락 */
    @PatchMapping("/{partnershipId}/accept")
    public ResponseEntity<ApiResponse<PartnershipPatchResponseDTO>> acceptInquiry(
            @PathVariable Long partnershipId) {
        PartnershipPatchResponseDTO dto = partnershipService.acceptInquiry(partnershipId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_ACCEPTED, dto));
    }

    /** 6. 받은 문의 거절 */
    @PatchMapping("/{partnershipId}/decline")
    public ResponseEntity<ApiResponse<PartnershipPatchResponseDTO>> declineInquiry(
            @PathVariable Long partnershipId) {
        PartnershipPatchResponseDTO dto = partnershipService.declineInquiry(partnershipId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_DECLINED, dto));
    }

    /** 7. 거절된 보낸 문의 삭제 */
    @DeleteMapping("/{partnershipId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeclinedInquiry(@PathVariable Long partnershipId) {
        partnershipService.deleteDeclinedInquiry(partnershipId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.PARTNERSHIP_DELETED, null));
    }
}
