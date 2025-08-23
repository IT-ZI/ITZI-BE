package com.itzi.itzi.agreement.controller;

import com.itzi.itzi.agreement.dto.request.AgreementRequestDTO;
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
     * 👉 프론트에서 입력 폼 작성 후 '임시 저장' 버튼 누를 때 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> createAgreement(
            @RequestBody AgreementRequestDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.createAgreement(dto)));
    }

    /**
     * 협약서 문서 변환
     * 상태: DRAFT → GENERATED
     * 👉 '문서 변환하기' 버튼을 누르면 호출됨
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> generateAgreement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.generateAgreement(id)));
    }

    /**
     * 송신자 서명
     * 상태: GENERATED → SIGNED_SENDER
     * 👉 작성자(보낸 사람)가 '서명하기' 버튼을 누르면 호출됨
     */
    @PostMapping("/{id}/sign/sender")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsSender(
            @PathVariable Long id, @RequestParam Long senderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsSender(id, senderId)));
    }

    /**
     * 문서 전송
     * 상태: SIGNED_SENDER → SENT
     * 👉 송신자가 서명한 뒤, '상대방에게 전송하기' 버튼을 누르면 호출됨
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> sendToReceiver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.sendToReceiver(id)));
    }

    /**
     * 수신자 서명
     * 상태: SENT → SIGNED_RECEIVER
     * 👉 문서를 받은 수신자가 '서명하기' 버튼을 누르면 호출됨
     */
    @PostMapping("/{id}/sign/receiver")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> signAsReceiver(
            @PathVariable Long id, @RequestParam Long receiverId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.signAsReceiver(id, receiverId)));
    }

    /**
     * 양측 서명 완료 처리
     * 상태: SIGNED_RECEIVER → SIGNED_ALL
     * 👉 수신자가 서명 완료 후, 시스템적으로 '양측 서명 완료' 처리
     */
    @PostMapping("/{id}/signed-all")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> markAllSigned(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.markAllSigned(id)));
    }

    /**
     * 최종 승인
     * 상태: SIGNED_ALL → APPROVED
     * 👉 모든 서명이 끝난 뒤, 최종적으로 '승인하기' 버튼을 누르면 호출됨
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AgreementDetailResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.approve(id)));
    }

    /**
     * Accepted/Approved 협약서 목록 조회
     * 👉 마이페이지에서 '협의 중(accepted)'과 '승인 완료(approved)' 내역을 동시에 보여줄 때 사용
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<?>>>> getAcceptedAndApproved(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getAcceptedAndApproved(userId)));
    }
}
