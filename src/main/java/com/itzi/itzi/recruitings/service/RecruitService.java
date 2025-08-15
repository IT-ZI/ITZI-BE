package com.itzi.itzi.recruitings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.recruitings.dto.response.RecruitingAiGenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitService {

    private final PostRepository postRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";


    public RecruitingAiGenerateResponse generateRecruitingAi(Long userId, Type type, String postImage, RecruitingAiGenerateRequest request) {

        // 1. 검증 : 날짜 역전 금지, 모든 필드 작성
        validate(type, request);

        // 2. 프롬프트 구성
        String prompt = buildPrompt(type, request);

        // 3. Gemini 호출
        String endpoint = GEMINI_ENDPOINT + "?key=" + apiKey;
        String content = callGemini(endpoint, prompt);

        // 4. 결과 저장
        Post saved = postRepository.save(
                Post.builder()
                        .type(Type.RECRUITING)
                        .postImage(StringUtils.hasText(postImage) ? postImage.trim() : null)
                        .title(request.getTitle().trim())
                        .target(request.getTarget().trim())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .benefit(request.getBenefit().trim())
                        .condition(request.getCondition().trim())
                        .content(content)
                        .targetNegotiable(request.getNegotiables() != null && Boolean.TRUE.equals(request.getNegotiables().getTarget()))
                        .periodNegotiable(request.getNegotiables() != null && Boolean.TRUE.equals(request.getNegotiables().getPeriod()))
                        .benefitNegotiable(request.getNegotiables() != null && Boolean.TRUE.equals(request.getNegotiables().getBenefit()))
                        .conditionNegotiable(request.getNegotiables() != null && Boolean.TRUE.equals(request.getNegotiables().getCondition()))
                        .exposureEndDate(request.getEndDate())                  // 수정 필요
                        .status(Status.DRAFT)
                        .build()
        );

        // 5. 응답 DTO
        return RecruitingAiGenerateResponse.builder()
                .postId(saved.getPostId())
                .userId(userId)
                .type(saved.getType())
                .postImage(saved.getPostImage())
                .title(saved.getTitle())
                .target(saved.getTarget())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .benefit(saved.getBenefit())
                .condition(saved.getCondition())
                .content(saved.getContent())
                .targetNegotiable(saved.isTargetNegotiable())
                .periodNegotiable(saved.isPeriodNegotiable())
                .benefitNegotiable(saved.isBenefitNegotiable())
                .conditionNegotiable(saved.isConditionNegotiable())
                .exposureEndDate(saved.getExposureEndDate())
                .status(saved.getStatus())
                .bookmarkCount(saved.getBookmarkCount())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }


    private void validate(Type type, RecruitingAiGenerateRequest request) {
        if (type == null) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (start != null && end != null && end.isBefore(start)) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID);

        }

        // 모든 텍스트 필드 필수
        if (!StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getTarget())
                || !StringUtils.hasText(request.getBenefit())
                || !StringUtils.hasText(request.getCondition())) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }
    }

    private String buildPrompt(Type type, RecruitingAiGenerateRequest r) {

        String postImageLine = (r.getPostImage() != null && !r.getPostImage().isBlank())
                ? "\n(이미지: " + r.getPostImage().trim() + ")"
                : "";

        // 제목, 타깃에서 학교명 자동 추출
        String school = extractSchoolName(r.getTitle())
                .or(() -> extractSchoolName(r.getTarget()))
                .orElse("00대학교");       // 기본값

        // 대상, 기간, 혜택, 조건 협의 가능 문구
        boolean targetOk = r.getNegotiables() != null && Boolean.TRUE.equals(r.getNegotiables().getTarget());
        boolean periodOk = r.getNegotiables() != null && Boolean.TRUE.equals(r.getNegotiables().getPeriod());
        boolean benefitOk = r.getNegotiables() != null && Boolean.TRUE.equals(r.getNegotiables().getBenefit());
        boolean condOk   = r.getNegotiables() != null && Boolean.TRUE.equals(r.getNegotiables().getCondition());
        String periodCondNote = (targetOk || periodOk || benefitOk || condOk ) ? " (대상, 기간, 혜택, 조건 협의 가능)" : "";

        return """
        너는 아래 '샘플 출력 양식'과 **완전히 동일한 레이아웃**으로 본문을 작성한다.
        - 생성할 때마다 다른 이모지를 적절하게 사용
        - 불필요한 접두/접미 문장, 설명, 따옴표, 코드블록 금지
        - 300~500자 내외, 문단은 샘플처럼 2개 본문 + 3개 섹션으로 구성
        - 아래 값으로 빈칸을 치환하여 최종 본문만 출력

        [입력 값]
        - 제목: %s
        - 대상: %s
        - 기간: %s ~ %s
        - 혜택: %s
        - 조건: %s
        - 타입: %s
        - 협의표시: %s

        [샘플 출력 양식]
        [%s]

        안녕하십니까, %s 총학생회입니다.%s
        저희 총학생회는 %s 동안, %s분들께 혜택을 제공해 주실 상권 제휴 매장을 모집하고 있습니다.

        이번 제휴는 지역 상권과 학교 구성원 간의 상생과 교류를 목적으로 하며,
        제휴를 맺어주시는 매장에는 적극적인 홍보를 통해 방문 유도와 인지도 향상을 도와드릴 예정입니다.
        제휴 혜택의 형태는 매장 상황과 특성에 맞추어 자유롭게 설정하실 수 있으며,
        할인, 쿠폰 제공, 사은품 증정, 시즌 이벤트 등 다양한 방식으로 협의가 가능합니다.

        📅 제휴 기간
        %s ~ %s%s

        🎯 제휴 대상
        %s

        💬 문의 안내
        제휴와 관련하여 궁금하신 사항이나 제안이 있으시면, 언제든 편하게 문의해 주시기 바랍니다.
        함께 의미 있는 제휴를 만들어갈 수 있기를 기대합니다!
        %s
        """.formatted(
                // [입력 값]
                r.getTitle().trim(),
                r.getTarget().trim(),
                r.getStartDate(), r.getEndDate(),
                r.getBenefit().trim(),
                r.getCondition().trim(),
                type.name(),
                periodCondNote,

                // [샘플 출력 양식] 치환 순서
                r.getTitle().trim(),                       // 제목
                school,                           // 총학생회 앞 학교명
                "",                                        // 총학생회 라인 뒤 추가 문구가 없다면 빈칸
                String.format("%s ~ %s", r.getStartDate(), r.getEndDate()),
                r.getTarget().trim(),
                r.getStartDate(), r.getEndDate(), periodCondNote,
                r.getTarget().trim(),
                postImageLine
        );
    }

    private String callGemini(String endpoint, String prompt) {
        try {
            if (!StringUtils.hasText(apiKey)) {
                throw new GeneralException(ErrorStatus.GEMINI_API_KEY_MISSING);
            }

            ObjectMapper om = new ObjectMapper();

            String body = "{"
                    + "\"contents\":[{"
                    + "  \"parts\":[{"
                    + "    \"text\":" + om.writeValueAsString(prompt)
                    + "  }]"
                    + "}]"
                    + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // HTTP 레벨 에러 : GEMINI_HTTP_ERROR
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new GeneralException(ErrorStatus.GEMINI_HTTP_ERROR,
                        "status=" + resp.statusCode() + ", body=" + resp.body());
            }

            JsonNode root = om.readTree(resp.body());

            // 안전성 차단
            JsonNode feedback = root.path("promptFeedback");
            if (!feedback.isMissingNode()) {
                String blockReason = feedback.path("blockReason").asText("");
                if (StringUtils.hasText(blockReason)) {
                    throw new GeneralException(ErrorStatus.GEMINI_BLOCKED,
                            blockReason + " / " + resp.body());
                }
            }

            // candidates/parts 포맷 검증 : GEMINI_INVALID_RESPONSE
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new GeneralException(ErrorStatus.GEMINI_INVALID_RESPONSE,
                        "AI 응답에 canditdates 누락: " + resp.body());
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new RuntimeException("AI 응답에 parts 누락: " + resp.body());
            }

            // 빈 텍스트를 반환했을 경우
            String text = parts.get(0).path("text").asText();
            if (!StringUtils.hasText(text)) {
                throw new GeneralException(ErrorStatus.GEMINI_EMPTY_TEXT, "body=" + resp.body());
            }
            return text.trim();

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, e.getMessage());

        }
    }

    // 학교명 추출
    private Optional<String> extractSchoolName(String text) {
        if ( text == null || text.isBlank()) return Optional.empty();

        Pattern p = Pattern.compile("[가-힣A-Za-z]{2,20}대학교");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

}