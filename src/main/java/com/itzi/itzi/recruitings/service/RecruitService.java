package com.itzi.itzi.recruitings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.recruitings.dto.request.RecruitingDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitService {

    private final PostRepository postRepository;
    private final S3Service s3Service;

    @Value("${gemini.api.key}")
    private String apiKey;

    private String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";


    public RecruitingAiGenerateResponse generateRecruitingAi(Long userId, Type type, RecruitingAiGenerateRequest request) {

        // 1. 검증 : 날짜 역전 금지, 모든 필드 작성
        validate(type, request);

        // 2. 프롬프트 구성
        String prompt = buildPrompt(type, request);

        // 3. Gemini 호출
        String endpoint = GEMINI_ENDPOINT + "?key=" + apiKey;
        String content = callGemini(endpoint, prompt);

        // 4. 이미지를 제외한 엔티티 구성
        Post entity = Post.builder()
                        .type(Type.RECRUITING)
                        .title(request.getTitle().trim())
                        .target(request.getTarget().trim())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .benefit(request.getBenefit().trim())
                        .condition(request.getCondition().trim())
                        .content(content)
                        .targetNegotiable(Boolean.TRUE.equals(request.getTargetNegotiable()))
                        .periodNegotiable(Boolean.TRUE.equals(request.getPeriodNegotiable()))
                        .benefitNegotiable(Boolean.TRUE.equals(request.getBenefitNegotiable()))
                        .conditionNegotiable(Boolean.TRUE.equals(request.getConditionNegotiable()))
                        .exposureEndDate(request.getExposureEndDate())
                        .status(Status.DRAFT)
                        .build();

        // 5. 이미지 업로드/변경
        handleImageUpload(entity, request.getPostImage());

        // 6. 저장
        Post saved = postRepository.save(entity);

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
        if (!hasText(request.getTitle())
                || !hasText(request.getTarget())
                || !hasText(request.getBenefit())
                || !hasText(request.getCondition())) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }
    }

    private String buildPrompt(Type type, RecruitingAiGenerateRequest r) {

        // MultipartFile 기준으로 존재 여부만 판단
        String postImageLine = (r.getPostImage() != null && !r.getPostImage().isEmpty())
                ? "\n(이미지 첨부됨)"   // 필요 없다면 "" 로 완전 제거해도 됨
                : "";

        // 제목, 타깃에서 학교명 자동 추출
        String school = extractSchoolName(r.getTitle())
                .or(() -> extractSchoolName(r.getTarget()))
                .orElse("00대학교");       // 기본값

        // 대상, 기간, 혜택, 조건 협의 가능 문구
        boolean targetOk  = Boolean.TRUE.equals(r.getTargetNegotiable());
        boolean periodOk  = Boolean.TRUE.equals(r.getPeriodNegotiable());
        boolean benefitOk = Boolean.TRUE.equals(r.getBenefitNegotiable());
        boolean condOk    = Boolean.TRUE.equals(r.getConditionNegotiable());
        String periodCondNote = (targetOk || periodOk || benefitOk || condOk ) ? " (대상, 기간, 혜택, 조건 협의 가능)" : "";

        return """
        너는 아래 '샘플 출력 양식'과 **완전히 동일한 레이아웃**으로 본문을 작성한다.
        - 이모지 사용 규칙
            1) 이모지 리스트 `[☺️😊😚🙌🏻🤝🏻🤙🏻🙏🏻🍀⭐️💌📍❗️️💬📢🕒]`에서 **3개를 무작위로 선택**
            2) **1문단과 2문단 본문 내용에서만** 적절히 배치
            3) 📅 제휴 기간, 🎯 제휴 대상, 💬 문의 안내 **섹션과 해당 섹션의 본문에는 이모지 사용 금지**
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

    @Transactional
    public RecruitingDraftSaveResponse saveOrUpdateDraft(Long userId, Type type, RecruitingDraftSaveRequest request) {

        // 임시 저장을 하기 위해서는 최소 1개 이상의 필드가 작성돼 있어야 함
        validateHasAnyDraftField(request);

        Post entity;
        if (request.getPostId() != null) {
            entity = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND, "존재하지 않는 postId입니다."));

            // status가 PUBLISHED, DELETED일 경우 임시 저장 불가
            if (entity.getStatus() != Status.DRAFT) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST, "임시 저장은 DRAFT 상태의 게시글에만 가능합니다.");
            }

            // 2) 부분 업데이트(널이면 무시, 값이 있으면 반영)
            applyPatch(entity, request);
            handleImageUpload(entity, request.getPostImage());

        } else {
            // 새 DRAFT 글 생성
            entity = Post.builder()
                    .status(Status.DRAFT)
                    .bookmarkCount(0L)
                    .build();
            applyPatch(entity, request);
            handleImageUpload(entity, request.getPostImage());
        }

        // 항상 DRAFT 상태 유지
        entity.setType(Type.RECRUITING);
        entity.setStatus(Status.DRAFT);

        Post saved = postRepository.save(entity);

        return new RecruitingDraftSaveResponse(
                Type.RECRUITING,
                saved.getPostId(),
                userId,
                saved.getStatus(),
                saved.getUpdatedAt()
        );
    }

    // 이미지 업로드/변경
    private void handleImageUpload(Post entity, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        try {
            // 기존 이미지가 존재한다면 삭제
            if (entity.getPostImage() != null && !entity.getPostImage().isBlank()) {
                s3Service.deleteImageUrl(entity.getPostImage());
            }

            String uploadUrl = s3Service.upload(file);
            entity.setPostImage(uploadUrl);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, "이미지 업로드에 실패했습니다.");
        }
    }

    // 임시 저장을 하기 위해서는 최소 1개 이상의 필드가 작성돼 있어야 함
    private void validateHasAnyDraftField(RecruitingDraftSaveRequest request) {
        boolean hasAny =
                request.getPostImage() != null && !request.getPostImage().isEmpty() ||
                hasText(request.getTitle()) ||
                hasText(request.getTarget()) ||
                request.getStartDate() != null || request.getEndDate() != null ||
                hasText(request.getBenefit()) || hasText(request.getCondition());

        if (!hasAny) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "임시 저장을 위해서는 1개 이상의 필드가 작성돼 있어야 합니다.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID);
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // 작성한 부분만 업데이트 (null이면 업데이트 X)
    private void applyPatch(Post e, RecruitingDraftSaveRequest request) {
        if (hasText(request.getTitle())) e.setTitle(request.getTitle());
        if (hasText(request.getTarget())) e.setTarget(request.getTarget());

        if (request.getStartDate() != null) e.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) e.setEndDate(request.getEndDate());

        if (hasText(request.getBenefit())) e.setBenefit(request.getBenefit());
        if (hasText(request.getCondition())) e.setCondition(request.getCondition());
        if (hasText(request.getContent())) e.setContent(request.getContent());

        if (request.getExposureEndDate() != null) e.setExposureEndDate(request.getExposureEndDate());

        if (request.getTargetNegotiable() != null) e.setTargetNegotiable(request.getTargetNegotiable());
        if (request.getPeriodNegotiable() != null) e.setPeriodNegotiable(request.getPeriodNegotiable());
        if (request.getTargetNegotiable() != null) e.setTargetNegotiable(request.getTargetNegotiable());
        if (request.getConditionNegotiable() != null) e.setConditionNegotiable(request.getConditionNegotiable());

    }

    // 제휴 홍보글 게시하기
    @Transactional
    public RecruitingPublishResponse publishRecruiting(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 이미 게시된 글인지 확인
        if (post.getStatus() == Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus.ALREADY_PUBLISHED);
        }

        // 제휴 모집글 게시를 위해서는 모든 필드가 작성돼야 함
        if (post.getPostImage() == null ||
            post.getTitle() == null || post.getTitle().isBlank() ||
            post.getTarget() == null || post.getTarget().isBlank() ||
            post.getStartDate() == null || post.getEndDate() == null ||
            post.getBenefit() ==  null || post.getBenefit().isBlank() ||
            post.getCondition() == null || post.getCondition().isBlank() ||
            post.getContent() == null || post.getContent().isBlank() ||
            post.getExposureEndDate() == null ) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }

        // 게시 상태로 변경 및 생성 시간 업데이트
        post.setStatus(Status.PUBLISHED);
        post.setCreatedAt(LocalDateTime.now());

        postRepository.save(post);

        return new RecruitingPublishResponse(
                Type.RECRUITING,
                post.getPostId(),
                post.getStatus(),
                post.getCreatedAt()
        );
    }

    // 제휴 홍보글 삭제하기
    @Transactional
    public RecruitingDeleteResponse deleteRecruiting(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 게시된 홍보글만 삭제 가능
        if (post.getStatus() == Status.DELETED || post.getStatus() == Status.DRAFT) {
            throw new GeneralException(ErrorStatus.CANNOT_DELETE_POST);
        }

        post.setStatus(Status.DELETED);
        postRepository.save(post);

        return new RecruitingDeleteResponse(
                Type.RECRUITING,
                post.getPostId(),
                post.getStatus()
        );
    }

    // 작성한 게시글 단건 상세 내용 조회
    @Transactional(readOnly = true)
    public RecruitingDetailResponse getRecruitingDetail(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        return RecruitingDetailResponse.builder()
                .userId(1L)                     // userId는 1로 고정
                .postId(post.getPostId())
                .type(post.getType())
                .status(post.getStatus())
                .exposureEndDate(post.getExposureEndDate())
                .bookmarkCount(post.getBookmarkCount())
                .title(post.getTitle())
                .target(post.getTarget())
                .targetNegotiable(post.isTargetNegotiable())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .periodNegotiable(post.isPeriodNegotiable())
                .benefit(post.getBenefit())
                .benefitNegotiable(post.isBenefitNegotiable())
                .condition(post.getCondition())
                .conditionNegotiable(post.isConditionNegotiable())
                .postImageUrl(post.getPostImage())
                .content(post.getContent())
                .build();
    }

    // 내가 작성한 게시글 전체 리스트 조회 (userId = 1 고정)
    @Transactional(readOnly = true)
    public List<RecruitingListResponse> getMyRecruitingList(Type type) {
        Long FIXED_USER_ID = 1L;
        List<Status> statuses = List.of(Status.DRAFT, Status.PUBLISHED);

        return postRepository.findByUserIdAndTypeAndStatusIn(FIXED_USER_ID, type, statuses)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    // 모든 사용자가 작성한 제휴 모집글 조회
    @Transactional(readOnly = true)
    public List<RecruitingListResponse> getAllRecruitingList(Type type, OrderBy orderBy) {
        Status status = Status.PUBLISHED;           // 게시된 게시물만 조회

        List<Post> posts = new ArrayList<>();

        // 기본 정렬 기준: 마감 임박순
        if (orderBy == null) {
            orderBy = OrderBy.CLOSING;
        }

        switch (orderBy) {
            case CLOSING -> {
                LocalDate today = LocalDate.now();
                posts = postRepository.findByTypeAndStatusAndExposureEndDateGreaterThanEqual(
                        type, status, today, Sort.by(Sort.Direction.ASC, "exposureEndDate")
                );
            }

            case POPULAR -> {
                posts = postRepository.findByTypeAndStatus(
                        type, status, Sort.by(Sort.Direction.DESC, "bookmarkCount"));
            }

            case LATEST -> {
                posts = postRepository.findByTypeAndStatus(
                        type, status, Sort.by(Sort.Direction.DESC, "createdAt"));
            }

            case OLDEST -> {
                posts = postRepository.findByTypeAndStatus(
                        type, status, Sort.by(Sort.Direction.ASC, "createdAt"));
            }
        }
        return posts.stream().map(this::toListResponse).toList();
    }


    private RecruitingListResponse toListResponse(Post post) {
        return RecruitingListResponse.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .type(post.getType())
                .status(post.getStatus())
                .exposureEndDate(post.getExposureEndDate())
                .bookmarkCount(post.getBookmarkCount())
                .postImageUrl(post.getPostImage())
                .title(post.getTitle())
                .target(post.getTarget())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .benefit(post.getBenefit())
                .targetNegotiable(post.isTargetNegotiable())
                .periodNegotiable(post.isPeriodNegotiable())
                .benefitNegotiable(post.isBenefitNegotiable())
                .build();
    }
}