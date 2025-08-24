package com.itzi.itzi.agreement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import com.itzi.itzi.agreement.dto.request.AgreementRequestDTO;
import com.itzi.itzi.agreement.dto.response.AcceptedPartnershipResponseDTO;
import com.itzi.itzi.agreement.dto.response.AgreementCalendarResponseDTO;
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

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 협약서 생성 (임시저장 상태)
     */
    public AgreementDetailResponseDTO createAgreement(AgreementRequestDTO dto) {
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("보낸 사용자 없음"));
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("받는 사용자 없음"));

        Partnership partnership = partnershipRepository.findById(dto.getPartnershipId())
                .orElseThrow(() -> new IllegalArgumentException("해당 partnership 없음"));

        if (partnership.getAcceptedStatus() != AcceptedStatus.ACCEPTED) {
            throw new IllegalStateException("제휴 문의가 수락된 상태에서만 협약서 생성이 가능합니다.");
        }

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

        LocalDate[] parsed = parsePeriod(dto.getTargetPeriod(), dto.getContent());
        agreement.setStartDate(parsed[0]);
        agreement.setEndDate(parsed[1]);

        agreementRepository.save(agreement);

        AgreementDetailResponseDTO responseDto = AgreementDetailResponseDTO.fromEntity(agreement);
        responseDto.setStartDate(parsed[0]);
        responseDto.setEndDate(parsed[1]);
        return responseDto;
    }

    /**
     * 협약서 수정 (DRAFT 상태에서만 가능)
     */
    public AgreementDetailResponseDTO updateAgreement(Long id, AgreementRequestDTO dto) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));

        if (agreement.getStatus() != Status.DRAFT) {
            throw new IllegalStateException("임시저장 상태(DRAFT)에서만 수정 가능합니다.");
        }

        // 필드 업데이트
        agreement.setSenderName(dto.getSenderName());
        agreement.setReceiverName(dto.getReceiverName());
        agreement.setPurpose(dto.getPurpose());
        agreement.setTargetPeriod(dto.getTargetPeriod());
        agreement.setBenefitCondition(dto.getBenefitCondition());
        agreement.setRole(dto.getRole());
        agreement.setEffect(dto.getEffect());
        agreement.setEtc(dto.getEtc());
        agreement.setContent(dto.getContent());

        LocalDate[] parsed = parsePeriod(dto.getTargetPeriod(), dto.getContent());
        agreement.setStartDate(parsed[0]);
        agreement.setEndDate(parsed[1]);

        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO generateAgreement(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));
        agreement.generate();
        return AgreementDetailResponseDTO.fromEntity(agreement);
    }

    public AgreementDetailResponseDTO generateAgreementAi(Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("해당 partnership 없음"));

        if (partnership.getAcceptedStatus() != AcceptedStatus.ACCEPTED) {
            throw new IllegalStateException("제휴 문의가 수락된 상태에서만 협약서 AI 생성이 가능합니다.");
        }

        Post post = partnership.getPost();
        User sender = partnership.getSender();
        User receiver = partnership.getReceiver();

        String prompt = buildAgreementPrompt(post, partnership);
        String raw = geminiService.callGemini(prompt);

        Map<String, String> parsed;
        try {
            parsed = objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            parsed = Map.of(
                    "purpose", partnership.getPurpose(),
                    "targetPeriod", partnership.getPeriodValue(),
                    "benefitCondition", partnership.getDetail(),
                    "role", "상호 협의된 역할과 의무",
                    "effect", "효력 관련 조항",
                    "etc", "기타 조항",
                    "content", raw
            );
        }

        Agreement agreement = Agreement.builder()
                .sender(sender)
                .receiver(receiver)
                .senderName(sender.getProfileName())
                .receiverName(receiver.getProfileName())
                .purpose(parsed.get("purpose"))
                .targetPeriod(parsed.get("targetPeriod"))
                .benefitCondition(parsed.get("benefitCondition"))
                .role(parsed.get("role"))
                .effect(parsed.get("effect"))
                .etc(parsed.get("etc"))
                .content(parsed.get("content"))
                .status(Status.DRAFT)
                .partnership(partnership)
                .post(post)
                .build();

        LocalDate[] parsedDates = parsePeriod(agreement.getTargetPeriod(), agreement.getContent());
        agreement.setStartDate(parsedDates[0]);
        agreement.setEndDate(parsedDates[1]);

        Agreement saved = agreementRepository.save(agreement);
        partnership.setAgreement(saved);

        AgreementDetailResponseDTO dto = AgreementDetailResponseDTO.fromEntity(saved);
        dto.setStartDate(parsedDates[0]);
        dto.setEndDate(parsedDates[1]);
        return dto;
    }

    private String buildAgreementPrompt(Post post, Partnership p) {
        return """
        너는 기업과 기관 간의 제휴 협약서를 작성하는 AI 비서야.
        JSON 형식으로만 응답하라.

        {
          "purpose": "...",
          "targetPeriod": "YYYY-MM-DD ~ YYYY-MM-DD",
          "benefitCondition": "...",
          "role": "...",
          "effect": "...",
          "etc": "...",
          "content": "전체 협약서 본문"
        }

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

    // --- 상태 전환 ---
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

    @Transactional(readOnly = true)
    public AgreementDetailResponseDTO getAgreementDetail(Long id) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("협약서 없음"));

        AgreementDetailResponseDTO dto = AgreementDetailResponseDTO.fromEntity(agreement);

        LocalDate[] parsed = parsePeriod(dto.getTargetPeriod(), dto.getContent());
        dto.setStartDate(parsed[0]);
        dto.setEndDate(parsed[1]);

        return dto;
    }

    private LocalDate[] parsePeriod(String targetPeriod, String content) {
        String text = (targetPeriod != null && !targetPeriod.isBlank())
                ? targetPeriod
                : (content != null ? content : "");

        if (text.isBlank()) {
            return new LocalDate[]{null, null};
        }

        try {
            Pattern KO_DATE = Pattern.compile("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
            Pattern ISO_DATE = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})");

            List<LocalDate> found = new ArrayList<>();

            Matcher mKo = KO_DATE.matcher(text);
            while (mKo.find()) {
                found.add(LocalDate.of(
                        Integer.parseInt(mKo.group(1)),
                        Integer.parseInt(mKo.group(2)),
                        Integer.parseInt(mKo.group(3))
                ));
                if (found.size() >= 2) break;
            }

            if (found.size() < 2) {
                Matcher mIso = ISO_DATE.matcher(text);
                while (mIso.find()) {
                    found.add(LocalDate.of(
                            Integer.parseInt(mIso.group(1)),
                            Integer.parseInt(mIso.group(2)),
                            Integer.parseInt(mIso.group(3))
                    ));
                    if (found.size() >= 2) break;
                }
            }

            if (found.size() >= 2) {
                LocalDate start = Collections.min(found);
                LocalDate end = Collections.max(found);
                return new LocalDate[]{start, end};
            }

        } catch (Exception ignored) { }

        return new LocalDate[]{null, null};
    }

    public List<AgreementCalendarResponseDTO> getApprovedAgreementsByMonth(Long userId, int year, int month) {
        // 해당 월의 시작일, 마지막일 계산
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        return agreementRepository.findByStatusAndSenderUserIdOrStatusAndReceiverUserId(
                        Status.APPROVED, userId,
                        Status.APPROVED, userId
                )
                .stream()
                .filter(a -> a.getStartDate() != null && a.getEndDate() != null)
                .filter(a -> !(a.getEndDate().isBefore(startOfMonth) || a.getStartDate().isAfter(endOfMonth)))
                // 👉 즉, 이번 달과 겹치는 기간만 남김
                .map(a -> AgreementCalendarResponseDTO.fromEntity(a, userId))
                .toList();
    }


}
