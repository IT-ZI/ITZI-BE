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
     * 협의 중인 항목 클릭 → partnershipId 기반으로 postId 반환
     * 👉 실제 Agreement 생성 전 준비 단계
     */
    @GetMapping("/prepare/{partnershipId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepareAgreement(
            @PathVariable Long partnershipId
    ) {
        Map<String, Object> response = agreementService.prepareAgreement(partnershipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 협약서 생성 (임시 저장, DRAFT)
     * 👉 수동 작성 버튼 눌렀을 때 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> createAgreement(
            @RequestBody AgreementRequestDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.createAgreement(dto)));
    }

    /**
     * 협약서 수정 (임시 저장 상태에서만 가능)
     * 👉 사용자가 값 입력 후 임시저장 버튼 눌렀을 때 호출
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> updateAgreement(
            @PathVariable Long id,
            @RequestBody AgreementRequestDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.updateAgreement(id, dto)));
    }

    /**
     * AI 기반 협약서 자동 생성
     * 👉 partnershipId 기준으로 AI가 작성한 초안 반환
     */
    @PostMapping("/ai/{partnershipId}")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> generateAgreementAi(
            @PathVariable Long partnershipId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                agreementService.generateAgreementAi(partnershipId)
        ));
    }

    /**
     * 협약서 문서 변환 (DRAFT → GENERATED)
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> generateAgreement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.generateAgreement(id)));
    }

    /**
     * 송신자 서명
     */
    @PatchMapping("/{id}/sign/sender")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsSender(
            @PathVariable Long id, @RequestParam Long senderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsSender(id, senderId)));
    }

    /**
     * 수신자에게 전송
     */
    @PatchMapping("/{id}/send")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> sendToReceiver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.sendToReceiver(id)));
    }

    /**
     * 수신자 서명
     */
    @PatchMapping("/{id}/sign/receiver")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsReceiver(
            @PathVariable Long id, @RequestParam Long receiverId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsReceiver(id, receiverId)));
    }

    /**
     * 양측 서명 완료
     */
    @PatchMapping("/{id}/signed-all")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> markAllSigned(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.markAllSigned(id)));
    }

    /**
     * 최종 승인
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.approve(id)));
    }

    /**
     * accepted / approved 조회
     * 👉 좌측(협의 중) / 우측(협의 완료) 리스트
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<?>>>> getAcceptedAndApproved(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getAcceptedAndApproved(userId)));
    }

    /**
     * 협약서 상세 조회
     * 👉 우측(협의 완료)에서 agreementId 클릭 시
     */
    @GetMapping("/detail/{id}") // ✅ /detail prefix 붙여서 충돌 제거
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> getAgreementDetail(
            @PathVariable Long id
    ) {
        AgreementDetailResponseDTO response = agreementService.getAgreementDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 캘린더 조회 (승인된 협약만)
     */
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
