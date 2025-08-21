package com.itzi.itzi.partnership.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.partnership.domain.AcceptedStatus;
import com.itzi.itzi.partnership.domain.Partnership;
import com.itzi.itzi.partnership.domain.SendStatus;
import com.itzi.itzi.partnership.dto.request.PartnershipPatchRequestDTO;
import com.itzi.itzi.partnership.dto.request.PartnershipPostRequestDTO;
import com.itzi.itzi.partnership.dto.response.PartnershipPatchResponseDTO;
import com.itzi.itzi.partnership.dto.response.PartnershipPostResponseDTO;
import com.itzi.itzi.partnership.repository.PartnershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnershipService {

    private final UserRepository userRepository;
    private final PartnershipRepository partnershipRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    /**
     * 1. AI 문의 글 변환 (POST)
     */
    public PartnershipPostResponseDTO postInquiry(
            Long userId, Long receiverId, PartnershipPostRequestDTO dto
    ) {
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // ✅ 이미 존재하는 DRAFT 또는 SEND 확인
        boolean exists = partnershipRepository.existsBySenderAndReceiver(sender, receiver);
        if (exists) {
            throw new GeneralException(ErrorStatus.ALREADY_SENT);
        }

        // 프롬프트 생성
        String prompt = buildPrompt(dto);

        // Gemini 호출
        String endpoint = GEMINI_ENDPOINT + "?key=" + apiKey;
        String aiContent = callGemini(endpoint, prompt);

        // 엔티티 저장 (AI 결과 포함, DRAFT 상태)
        Partnership partnership = Partnership.builder()
                .sender(sender)
                .receiver(receiver)
                .purpose(dto.getPurpose())
                .periodType(dto.getPeriodType())
                .periodValue(dto.getPeriodValue())
                .orgType(dto.getOrgType())
                .orgValue(dto.getOrgValue())
                .detail(dto.getDetail())
                .keywords(dto.getKeywords())
                .content(aiContent)
                .sendStatus(SendStatus.DRAFT)   // 초안
                .acceptedStatus(AcceptedStatus.WAITING) // 대기
                .build();

        Partnership saved = partnershipRepository.save(partnership);
        return PartnershipPostResponseDTO.fromEntity(saved);
    }

    /**
     * 2. 문의 보내기 (PATCH)
     */
    public PartnershipPatchResponseDTO patchInquiry(
            Long partnershipId, PartnershipPatchRequestDTO dto
    ) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PARTNERSHIP_NOT_FOUND));

        // 이미 전송된 경우 차단
        if (partnership.getSendStatus() == SendStatus.SEND) {
            throw new GeneralException(ErrorStatus.ALREADY_SENT);
        }

        // content 수정값 있으면 반영
        if (dto != null && dto.getContent() != null && !dto.getContent().isBlank()) {
            partnership.setContent(dto.getContent());
        }

        // status 전환
        partnership.setSendStatus(SendStatus.SEND);

        Partnership updated = partnershipRepository.save(partnership);
        return PartnershipPatchResponseDTO.fromEntity(updated);
    }


    /**
     * 3. 내가 보낸 문의 조회
     */
    public List<PartnershipPostResponseDTO> getSentInquiries(Long userId) {
        List<Partnership> list = partnershipRepository.findBySenderUserId(userId);
        return list.stream().map(PartnershipPostResponseDTO::fromEntity).toList();
    }

    /**
     * 4. 내가 받은 문의 조회
     */
    public List<PartnershipPostResponseDTO> getReceivedInquiries(Long userId) {
        List<Partnership> list = partnershipRepository.findByReceiverUserId(userId);
        return list.stream().map(PartnershipPostResponseDTO::fromEntity).toList();
    }

    /**
     * 5. 받은 문의 수락
     */
    public PartnershipPatchResponseDTO acceptInquiry(Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PARTNERSHIP_NOT_FOUND));

        if (partnership.getSendStatus() != SendStatus.SEND) {
            throw new GeneralException(ErrorStatus.INVALID_STATUS, "아직 전송되지 않은 문의입니다.");
        }

        partnership.setAcceptedStatus(AcceptedStatus.ACCEPTED);
        Partnership updated = partnershipRepository.save(partnership);
        return PartnershipPatchResponseDTO.fromEntity(updated);
    }

    /**
     * 6. 받은 문의 거절
     */
    public PartnershipPatchResponseDTO declineInquiry(Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PARTNERSHIP_NOT_FOUND));

        if (partnership.getSendStatus() != SendStatus.SEND) {
            throw new GeneralException(ErrorStatus.INVALID_STATUS, "아직 전송되지 않은 문의입니다.");
        }

        partnership.setAcceptedStatus(AcceptedStatus.DECLINED);
        Partnership updated = partnershipRepository.save(partnership);
        return PartnershipPatchResponseDTO.fromEntity(updated);
    }

    /**
     * 7. 거절된 보낸 문의 삭제
     */
    public void deleteDeclinedInquiry(Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PARTNERSHIP_NOT_FOUND));

        if (partnership.getAcceptedStatus() != AcceptedStatus.DECLINED) {
            throw new GeneralException(ErrorStatus.NOT_ALLOWED_DELETE);
        }

        partnershipRepository.delete(partnership);
    }

    /**
     * Gemini 프롬프트 구성
     */
    private String buildPrompt(PartnershipPostRequestDTO dto) {
        return """
        당신은 제휴 요청 홍보 담당자입니다.
        아래 입력값을 참고하여 제휴 제안/홍보글을 작성하세요.

        [제휴 요청 정보]
        - 제휴 목적: %s
        - 제휴 기간: %s %s
        - 우리 단체: %s
        - 세부 내용: %s
        - 요청 키워드: %s

        [작성 규칙]
        1) 분량: 300~500자
        2) 문단: 인사/소개 → 제휴 필요성 → 기대 효과/혜택 → 마무리
        3) 문체: 요청 키워드 반영 (예: 친절함, 간결함, 예의, 설득력, 따뜻함 등)
        4) 이모지: ☺️😊😚🙌🏻🤝🏻🤙🏻🙏🏻🍀⭐️💌📍❗️💬📢🕒 중 2~3개 사용 (본문 안에서)
        5) 불필요한 설명, 코드블록, 따옴표 금지
        """.formatted(
                dto.getPurpose(),
                dto.getPeriodValue(), dto.getPeriodType(),
                dto.getOrgValue(),
                dto.getDetail(),
                dto.getKeywords() != null ? String.join(", ", dto.getKeywords()) : ""
        );
    }

    /**
     * Gemini API 호출
     */
    private String callGemini(String endpoint, String prompt) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new GeneralException(ErrorStatus.GEMINI_API_KEY_MISSING);
            }

            ObjectMapper om = new ObjectMapper();
            String body = "{"
                    + "\"contents\":[{"
                    + "  \"parts\":[{"
                    + "    \"text\":" + om.writeValueAsString(prompt)
                    + "  }]}]}"
                    ;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new GeneralException(ErrorStatus.GEMINI_HTTP_ERROR,
                        "status=" + resp.statusCode() + ", body=" + resp.body());
            }

            JsonNode root = om.readTree(resp.body());
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                throw new GeneralException(ErrorStatus.GEMINI_INVALID_RESPONSE,
                        "AI 응답에 parts 누락: " + resp.body());
            }

            String text = parts.get(0).path("text").asText();
            if (text == null || text.isBlank()) {
                throw new GeneralException(ErrorStatus.GEMINI_EMPTY_TEXT, "body=" + resp.body());
            }

            return text.trim();

        } catch (Exception e) {
            log.error("Gemini 호출 실패", e);
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, e.getMessage());
        }
    }
}
