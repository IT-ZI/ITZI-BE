package com.itzi.itzi.agreement.controller;

import com.itzi.itzi.agreement.dto.request.AgreementRequestDTO;
import com.itzi.itzi.agreement.dto.response.AgreementCalendarResponseDTO;
import com.itzi.itzi.agreement.dto.response.AgreementDetailResponseDTO;
import com.itzi.itzi.agreement.service.AgreementService;
import com.itzi.itzi.global.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agreements") // 모든 API가 /agreements 로 시작
@RequiredArgsConstructor
@Slf4j
public class AgreementController {

    private final AgreementService agreementService;

    /**
     * 협약서 생성 (임시 저장)
     * 상태: DRAFT
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> createAgreement(
            @RequestBody AgreementRequestDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.createAgreement(dto)));
    }

    /**
     * 협약서 수정 (임시 저장 상태에서만 가능)
     * 상태: DRAFT 유지
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> updateAgreement(
            @PathVariable Long id,
            @RequestBody AgreementRequestDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.updateAgreement(id, dto)));
    }

    @PostMapping("/ai/{partnershipId}")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> generateAgreementAi(
            @PathVariable Long partnershipId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                agreementService.generateAgreementAi(partnershipId)
        ));
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> generateAgreement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.generateAgreement(id)));
    }

    @PatchMapping("/{id}/sign/sender")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsSender(
            @PathVariable Long id, @RequestParam Long senderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsSender(id, senderId)));
    }

    @PatchMapping("/{id}/send")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> sendToReceiver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.sendToReceiver(id)));
    }

    @PatchMapping("/{id}/sign/receiver")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsReceiver(
            @PathVariable Long id, @RequestParam Long receiverId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsReceiver(id, receiverId)));
    }

    @PatchMapping("/{id}/signed-all")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> markAllSigned(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.markAllSigned(id)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.approve(id)));
    }

    // accepted or approved 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<?>>>> getAcceptedAndApproved(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getAcceptedAndApproved(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> getAgreementDetail(
            @PathVariable Long id
    ) {
        AgreementDetailResponseDTO response = agreementService.getAgreementDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/approved/calendar")
    public ResponseEntity<ApiResponse<List<AgreementCalendarResponseDTO>>> getApprovedAgreementsByMonth(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<AgreementCalendarResponseDTO> list = agreementService.getApprovedAgreementsByMonth(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(list));
    }


}
