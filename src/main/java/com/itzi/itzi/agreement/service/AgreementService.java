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
import com.itzi.itzi.global.gemini.GeminiService;
import com.itzi.itzi.partnership.domain.AcceptedStatus;
import com.itzi.itzi.partnership.domain.Partnership;
import com.itzi.itzi.partnership.dto.response.PartnershipPostResponseDTO;
import com.itzi.itzi.partnership.repository.PartnershipRepository;
import com.itzi.itzi.posts.domain.Post;
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
    private final GeminiService geminiService;

    /**
     * 협약서 생성 (임시저장 상태)
     * 상태: DRAFT
     */
    public AgreementDetailResponseDTO createAgreement(AgreementRequestDTO dto) {
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("보낸 사용자 없음"));
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("받는 사용자 없음"));

        // 2. partnership 조회 (❗필수)
        Partnership partnership = partnershipRepository.findById(dto.getPartnershipId())
                .orElseThrow(() -> new IllegalArgumentException("해당 partnership 없음"));

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
                .status(Status.DRAFT)
                .partnership(partnership)
                .post(partnership.getPost())
                .build();

        agreementRepository.save(agreement);

        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 협약서 문서 변환 (Draft → Generated)
     * 👉 사용자가 직접 문서 변환 버튼을 눌렀을 때
     */
    public AgreementDetailResponseDTO generateAgreement(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.generate();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * 협약서 문서 변환 (AI 자동 작성)
     * 👉 관련 모집글 + 문의글 기반으로 AI가 초안 생성
     */
    public AgreementDetailResponseDTO generateAgreementAi(Long partnershipId) {

        // 1. 관련 제휴 문의글 조회
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("해당 partnership 없음"));

        // 2. 모집글(Post) + 송수신자
        Post post = partnership.getPost();
        User sender = partnership.getSender();
        User receiver = partnership.getReceiver();

        // 3. 프롬프트 생성
        String prompt = buildAgreementPrompt(post, partnership);

        // 4. Gemini 호출
        String content = geminiService.callGemini(prompt);

        // 5. Agreement 엔티티 생성
        Agreement agreement = Agreement.builder()
                .sender(sender)
                .receiver(receiver)
                .senderName(sender.getProfileName())
                .receiverName(receiver.getProfileName())
                .purpose(partnership.getPurpose())
                .targetPeriod(partnership.getPeriodValue())
                .benefitCondition(partnership.getDetail())
                .role("상호 협의된 역할과 의무를 따른다.")
                .effect("협약 해지 및 효력 관련 조항을 따른다.")
                .etc("기타 필요한 조항 포함 가능")
                .content(content) // AI 생성 본문
                .status(Status.DRAFT)
                .partnership(partnership) // ✅ 연결
                .build();

        // 6. 저장
        Agreement saved = agreementRepository.save(agreement);
        partnership.setAgreement(saved);

        return AgreementDetailResponseDTO.fromEntity(saved);
    }

    private String buildAgreementPrompt(Post post, Partnership p) {
        return """
        너는 기업과 기관 간의 제휴 협약서를 작성하는 AI 비서야.
        아래 '제휴 모집글'과 '제휴 문의글' 정보를 기반으로
        1차 협약서 초안을 작성해라.

        - 형식: 제1조(목적), 제2조(대상 및 기간), 제3조(혜택 및 조건), 제4조(역할 및 의무), 제5조(효력 및 해지), 제6조(기타)
        - 각 조항은 2~3문장으로 작성
        - 불필요한 인사말/설명은 제외
        - 출력은 협약서 본문만

        [제휴 모집글 정보]
        제목: %s
        대상: %s
        기간: %s ~ %s
        혜택: %s
        조건: %s

        [제휴 문의글 정보]
        목적: %s
        상세 내용: %s
        기간: %s
        """.formatted(
                post.getTitle(),
                post.getTarget(),
                post.getStartDate(), post.getEndDate(),
                post.getBenefit(), post.getCondition(),
                p.getPurpose(),
                p.getDetail(),
                p.getPeriodValue()
        );
    }

    // --- 이하 상태 전환 메서드들 (기존과 동일) ---

    public AgreementDetailResponseDTO signAsSender(Long agreementId, Long senderId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        agreement.signAsSender(sender);
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO sendToReceiver(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.sendToReceiver();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO signAsReceiver(Long agreementId, Long receiverId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        agreement.signAsReceiver(receiver);
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO markAllSigned(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.markAllSigned();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO approve(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.approve();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    /**
     * Accepted/Approved 협약서 목록 조회
     */
    public Map<String, List<?>> getAcceptedAndApproved(Long userId) {
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
