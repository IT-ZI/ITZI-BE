package com.itzi.itzi.agreement.service;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import com.itzi.itzi.agreement.dto.request.AgreementRequestDTO;
import com.itzi.itzi.agreement.dto.response.AcceptedPartnershipResponseDTO;
import com.itzi.itzi.agreement.dto.response.AgreementDetailResponseDTO;
import com.itzi.itzi.agreement.dto.response.AgreementResponseDTO;
import com.itzi.itzi.agreement.repository.AgreementRepository;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.partnership.domain.AcceptedStatus;
import com.itzi.itzi.partnership.dto.response.PartnershipPostResponseDTO;
import com.itzi.itzi.partnership.repository.PartnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;

    /**
     * 협약서 생성 (임시저장 상태)
     * 상태: DRAFT
     */
    public AgreementDetailResponseDTO createAgreement(AgreementRequestDTO dto) {
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("보낸 사용자 없음"));
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("받는 사용자 없음"));

        Agreement agreement = Agreement.builder()
                .sender(sender)
                .receiver(receiver)
                .senderName(dto.getSenderName())
                .receiverName(dto.getReceiverName())
                .purpose(dto.getPurpose())
                .targetPeriod(dto.getTargetPeriod())
                .benefitCondition(dto.getBenefitCondition())
                .role(dto.getRole())
                .effect(dto.getEffect())
                .etc(dto.getEtc())
                .content(dto.getContent())
                .status(Status.DRAFT) // 새로 생성 시 무조건 DRAFT
                .build();

        agreementRepository.save(agreement);

        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 협약서 문서 변환 (Draft → Generated)
     */
    public AgreementDetailResponseDTO generateAgreement(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.generate();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 송신자 서명 (Generated → Signed_Sender)
     */
    public AgreementDetailResponseDTO signAsSender(Long agreementId, Long senderId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        agreement.signAsSender(sender);
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 문서 전송 (Signed_Sender → Sent)
     */
    public AgreementDetailResponseDTO sendToReceiver(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.sendToReceiver();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 수신자 서명 (Sent → Signed_Receiver)
     */
    public AgreementDetailResponseDTO signAsReceiver(Long agreementId, Long receiverId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        agreement.signAsReceiver(receiver);
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 양측 모두 서명 완료 (Signed_Receiver → Signed_All)
     */
    public AgreementDetailResponseDTO markAllSigned(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.markAllSigned();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 최종 승인 (Signed_All → Approved)
     */
    public AgreementDetailResponseDTO approve(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.approve();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * Accepted/Approved 협약서 목록 조회
     * 👉 목록 조회는 간단하게 AgreementResponseDTO 그대로 유지
     */
    public Map<String, List<?>> getAcceptedAndApproved(Long userId) {
        // Accepted 목록
        List<AcceptedPartnershipResponseDTO> acceptedList =
                partnershipRepository.findByAcceptedStatusAndSenderUserIdOrAcceptedStatusAndReceiverUserId(
                                AcceptedStatus.ACCEPTED, userId,
                                AcceptedStatus.ACCEPTED, userId
                        )
                        .stream()
                        .map(p -> {
                            boolean isSender = p.getSender().getUserId().equals(userId);
                            User partner = isSender ? p.getReceiver() : p.getSender();
                            return AcceptedPartnershipResponseDTO.builder()
                                    .partnershipId(p.getPartnershipId())
                                    .partnerDisplayName(
                                            PartnershipPostResponseDTO.resolveDisplayName(partner)
                                    )
                                    .build();
                        })
                        .toList();


        // Approved 목록
        List<AgreementResponseDTO> approvedList =
                agreementRepository.findByStatusAndSenderUserIdOrStatusAndReceiverUserId(
                                Status.APPROVED, userId,
                                Status.APPROVED, userId
                        )
                        .stream()
                        .map(a -> AgreementResponseDTO.fromEntity(a, userId))
                        .toList();

        Map<String, List<?>> result = new HashMap<>();
        result.put("Accepted", acceptedList);
        result.put("Approved", approvedList);
        return result;
    }
}
