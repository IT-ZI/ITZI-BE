package com.itzi.itzi.global.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasText;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public String callGemini(String prompt) {
        try {
            if (!hasText(apiKey)) {
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
                    .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
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
                if (hasText(blockReason)) {
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
            if (!hasText(text)) {
                throw new GeneralException(ErrorStatus.GEMINI_EMPTY_TEXT, "body=" + resp.body());
            }

            Pattern jsonPattern = Pattern.compile("\\{[^{}]*\\}");
            Matcher matcher = jsonPattern.matcher(text);

            if (matcher.find()) {
                String jsonResult = matcher.group();
                // 최종적으로 추출된 문자열이 유효한 JSON인지 검증
                try {
                    om.readTree(jsonResult);
                    return jsonResult.trim();
                } catch (IOException e) {
                    throw new GeneralException(ErrorStatus.GEMINI_INVALID_RESPONSE,
                            "추출된 문자열이 유효한 JSON이 아닙니다: " + jsonResult);
                }
            } else {
                throw new GeneralException(ErrorStatus.GEMINI_INVALID_RESPONSE,
                        "AI 응답에서 JSON 객체를 찾을 수 없습니다: " + text);
            }

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, e.getMessage());

        }
    }

    // 학교명 추출
    public Optional<String> extractSchoolName(String text) {
        if ( text == null || text.isBlank()) return Optional.empty();

        Pattern p = Pattern.compile("([가-힣A-Za-z]{2,20})대학교");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

}