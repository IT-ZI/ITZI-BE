package com.itzi.itzi.promotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.repository.AgreementRepository;
import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.OrgProfileRepository;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.dto.response.PostListResponse;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.posts.service.PostService;
import com.itzi.itzi.promotion.dto.request.PromotionAiGenerateRequest;
import com.itzi.itzi.promotion.dto.request.PromotionDraftSaveRequest;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {

    private final PostRepository postRepository;
    private final S3Service s3Service;
    private final AgreementRepository agreementRepository;
    private final PostService postService;
    private final UserRepository userRepository;
    private final OrgProfileRepository orgProfileRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    // 제휴 홍보 게시글을 맺을 수 있는 제휴 대상자 리스트 조회
    @Transactional(readOnly = true)
    public List<String> getAvailableAgreement() {
        List<Agreement> agreementList = agreementRepository.findByStatus(com.itzi.itzi.agreement.domain.Status.APPROVED);

        return agreementList.stream()
                .map(Agreement::getReceiverName) // Agreement 객체에서 receiverName만 추출
                .distinct() // 중복 제거
                .collect(Collectors.toList());
    }

    // 제휴 홍보 게시글 AI 자동 작성
    @Transactional
    public PromotionAiGenerateResponse generatePromotion(Long userId, PromotionAiGenerateRequest request) {

        // 1. 제휴 상태 검증 및 agreement 데이터 조회
        Agreement agreement = agreementRepository.findById(request.getAgreementId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.AGREEMENT_NOT_FOUND, "해당하는 제휴 정보를 찾을 수 없습니다."));

        if (agreement.getStatus() != com.itzi.itzi.agreement.domain.Status.APPROVED) {
            throw new GeneralException(ErrorStatus.INVALID_STATUS, "승인된 제휴만 홍보 게시글을 생성할 수 있습니다.");
        }

        // 2. 이미 PROMOTION 타입의 Post가 존재하는지 확인
        Optional<Post> existingPromotionPost
                = postRepository.findByAgreement_AgreementIdAndType(agreement.getAgreementId(), Type.PROMOTION);

        if (existingPromotionPost.isPresent()) {
            throw new GeneralException(ErrorStatus.POST_ALREADY_EXISTS, "이미 제휴 홍보 게시글이 존재합니다.");
        }

        // 3. AI에게 보낼 프롬프트 구성에 필요한 기존 RECRUITING Post 조회
        // agreement 엔티티를 사용해서 post 조회
        Post recruitingPost = agreement.getPost();
        if (recruitingPost == null || recruitingPost.getType() != Type.RECRUITING) {
            throw new GeneralException(ErrorStatus.NOT_FOUND, "연결된 모집 게시글(RECRUITING)을 찾을 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        OrgProfile orgProfile = orgProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 4. Agreement 문서 내용 파싱
        Map<String, String> agreementData = parseAgreementContent(agreement.getContent());

        // 5. 프롬프트 구성
        String prompt = buildPrompt(agreementData, recruitingPost);

        // 6. AI API 호출 및 JSON 응답 파싱 (이전 답변대로 sanitize + lenient parser 적용 권장)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        {
            String geminiRaw  = callGemini(prompt);
            String geminiJson = sanitizeModelTextToJson(geminiRaw);
            rootNode = parseJsonStrictFirstThenLenient(mapper, geminiJson);
        }

        String generatedTitle   = rootNode.path("title").asText("기본 제목");
        String generatedContent = rootNode.path("content").asText("기본 내용");

        // 7. 기간 결정 (Agreement의 명시 필드를 최우선)
        LocalDate startDate = agreement.getStartDate();
        LocalDate endDate   = agreement.getEndDate();

        if (startDate == null || endDate == null) {
            // 필드가 비어있다면 문자열에서 파싱 시도
            String rawPeriod = Stream.of(
                            agreement.getTargetPeriod(),
                            agreementData.get("period"),
                            agreementData.get("target_period"),
                            // 모집글의 start/end가 있으면 Fallback
                            (recruitingPost.getStartDate() != null && recruitingPost.getEndDate() != null)
                                    ? recruitingPost.getStartDate() + "~" + recruitingPost.getEndDate()
                                    : null,
                            agreement.getContent(),
                            recruitingPost.getContent()
                    ).filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .findFirst()
                    .orElse("");

            DateRange dr = parseDateRangeFlexible(rawPeriod,
                    recruitingPost.getStartDate() != null ? recruitingPost.getStartDate() : LocalDate.now());
            startDate = dr.start;
            endDate   = dr.end;
        }

        if (endDate.isBefore(startDate)) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID, "종료일이 시작일보다 빠릅니다.");
        }

        // 8. 새로운 PROMOTION 타입의 Post 엔티티 생성
        Post newPromotionPost = Post.builder()
                .type(Type.PROMOTION)
                .status(Status.DRAFT)
                .user(user)
                .title(generatedTitle)
                .postImage(user.getProfileImage())                  // ai 반환 시 기본 이미지는 프로필 이미지로 사용
                .content(generatedContent)
                .startDate(startDate)
                .endDate(endDate)
                .target(agreementData.getOrDefault("target", "대상 미정"))
                .benefit(agreementData.getOrDefault("benefit", "혜택 미정"))
                .condition(agreementData.getOrDefault("condition", "조건 미정"))
                .agreement(agreement)
                .exposeTargetInfo(true)
                .exposeProposerInfo(true)
                .exposureEndDate(endDate)
                .orgProfile(orgProfile)
                .build();

        Post saved = postRepository.save(newPromotionPost);

        // 9. 응답 DTO 반환
        return PromotionAiGenerateResponse.builder()
                .postId(saved.getPostId())
                .userId(userId)
                .type(Type.PROMOTION)
                .status(Status.DRAFT)
                .title(saved.getTitle())
                .target(saved.getTarget())
                .period(startDate + "~" + endDate)
                .benefit(saved.getBenefit())
                .condition(saved.getCondition())
                .content(saved.getContent())
                .build();
    }

    // 제휴 게시글 수동 작성 후 업로드
    @Transactional
    public PromotionManualPublishResponse promotionManualPublish(Long userId, Long agreementId, PromotionManualPublishRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        OrgProfile orgProfile = orgProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 0. 제휴 게시글을 맺을 수 있는 상태인지 검증
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.AGREEMENT_NOT_FOUND));

        // agreement 협의 상태가 완료인 경우에만 제휴 홍보글 작성 가능
        if (agreement.getStatus() != com.itzi.itzi.agreement.domain.Status.APPROVED) {
            throw new GeneralException(ErrorStatus.INVALID_STATUS);
        }

        // Agreement에 연결된 Post가 이미 있는지 확인
        if (agreement.getPost() != null && agreement.getPost().getType() == Type.PROMOTION) {
            throw new GeneralException(ErrorStatus.POST_ALREADY_EXISTS);
        }

        if (agreement.getSender() == null) {
            throw new GeneralException(ErrorStatus.NOT_FOUND, "작성자 정보가 누락되었습니다.");
        }

        // 1. 필수 값 검증
        validateForPublish(request);

        // 2. 엔티티 생성 및 저장
        Post post = Post.builder()
                .type(Type.PROMOTION)
                .status(Status.PUBLISHED)
                .user(user)
                .orgProfile(orgProfile)
                .user(agreement.getSender())
                .sender(agreement.getSender())
                .receiver(agreement.getReceiver())
                .title(request.getTitle())
                .target(request.getTarget())
                .benefit(request.getBenefit())
                .condition(request.getCondition())
                .content(request.getContent())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .exposureEndDate(request.getExposureEndDate())
                .exposeProposerInfo(Boolean.TRUE.equals(request.getExposeProposerInfo()))
                .exposeTargetInfo(Boolean.TRUE.equals(request.getExposeTargetInfo()))
                .agreement(agreement)
                .publishedAt(LocalDateTime.now())
                .build();

        // 3. 이미지 업로드
        handleImageUpload(post, request.getPostImage());

        postRepository.save(post);

        // agreement 엔티티에 Post 정보 업데이트 (양방향 관계)
        agreement.setPost(post);
        agreementRepository.save(agreement);

        // 4. 응답 반환
        return PromotionManualPublishResponse.builder()
                .type(post.getType())
                .status(post.getStatus())
                .postId(post.getPostId())
                .senderName(agreement.getSenderName())
                .receiverName(agreement.getReceiverName())
                .postImage(post.getPostImage())
                .title(post.getTitle())
                .target(post.getTarget())
                .benefit(post.getBenefit())
                .condition(post.getCondition())
                .content(post.getContent())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .exposureEndDate(post.getExposureEndDate())
                .exposeProposerInfo(post.getExposeProposerInfo())
                .exposeTargetInfo(post.getExposeTargetInfo())
                .publishedAt(post.getPublishedAt())
                .build();

    }

    // 제휴 게시글 임시 저장
    @Transactional
    public PromotionDraftSaveResponse promotionDraft(Long userId, PromotionDraftSaveRequest request) {

        // 1. 제휴 상태 검증 및 agreement 데이터 조회
        Agreement agreement = agreementRepository.findById(request.getAgreementId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.AGREEMENT_NOT_FOUND, "해당하는 제휴 정보를 찾을 수 없습니다."));

        if (agreement.getStatus() != com.itzi.itzi.agreement.domain.Status.APPROVED) {
            throw new GeneralException(ErrorStatus.INVALID_STATUS, "승인된 제휴만 홍보 게시글을 생성할 수 있습니다.");
        }

        // 2. 이미 PROMOTION 타입의 Post가 존재하는지 확인
        Optional<Post> existingPromotionPost
                = postRepository.findByAgreement_AgreementIdAndType(agreement.getAgreementId(), Type.PROMOTION);

        if (existingPromotionPost.isPresent()) {
            throw new GeneralException(ErrorStatus.POST_ALREADY_EXISTS, "이미 제휴 홍보 게시글이 존재합니다.");
        }

        // 1. 필수 값 검증
        validateHasAnyDraftField(request);

        // 2. 날짜 역전 검증
        validateOptionalDateRange(request.getStartDate(), request.getEndDate());

        Post post;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));

        if (request.getPostId() != null) {
            post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND, "존재하지 않는 postId입니다."));

            // status가 DRAFT일 경우에만 임시 저장 가능
            if (post.getStatus() != Status.DRAFT) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST, "임시 저장은 DRAFT 상태의 게시글만 가능합니다.");

            }

            // 수정, 새로 작성된 부분만 업데이트
            applyPatch(post, request);
            handleImageUpload(post, request.getPostImage());
        } else {
            // 새 제휴 게시글 생성
            post = Post.builder()
                    .status(Status.DRAFT)
                    .user(user)
                    .user(User.builder().userId(userId).build())
                    .type(Type.PROMOTION)
                    .build();

            applyPatch(post, request);
            handleImageUpload(post, request.getPostImage());
        }

        if (post.getType() == null) {
            post.setType(Type.PROMOTION);
        }

        Post saved = postRepository.save(post);

        return new PromotionDraftSaveResponse(
                saved.getPostId(),
                userId,
                Type.PROMOTION,
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    // 제휴 게시글 업로드 (최초)
    @Transactional
    public PromotionPublishResponse publish(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 이미 게시된 글인지 확인
        if (post.getStatus() == Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus.ALREADY_PUBLISHED);
        }

        // 제휴 모집글 게시를 위해서는 모든 필드가 작성돼야 함
        validateForPublishEntity(post);


        // 게시 상태로 변경 및 생성 시간 업데이트
        post.setStatus(Status.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());

        postRepository.save(post);

        return new PromotionPublishResponse(
                Type.RECRUITING,
                post.getStatus(),
                post.getPostId(),
                post.getPublishedAt()
        );
    }

    // 재수정 진입 (작성된 값이 화면에 조회)
    @Transactional(readOnly = true)
    public PromotionEditViewResponse getEditView(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        if (post.getStatus() != Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST, "게시된 게시물만 수정 가능합니다.");
        }

        return PromotionEditViewResponse.builder()
                .postId(post.getPostId())
                .postImage(post.getPostImage())
                .title(post.getTitle())
                .target(post.getTarget())
                .benefit(post.getBenefit())
                .condition(post.getCondition())
                .content(post.getContent())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .exposureEndDate(post.getExposureEndDate())
                .exposeProposerInfo(post.getExposeProposerInfo())
                .exposeTargetInfo(post.getExposeTargetInfo())
                .build();
    }

    // 재수정 후 즉시 게시
    @Transactional
    public PromotionManualPublishResponse republish(Long userId, Long postId, PromotionManualPublishRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        if (post.getStatus() != Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST, "게시된 글만 재수정 가능합니다.");
        }

        // 1. 수정된 값 반영
        applyPatch(post, request);

        // 2. 이미지가 변경된 경우 교체
        if (request.getPostImage() != null && !request.getPostImage().isEmpty()) {
            handleImageUpload(post, request.getPostImage());
        }

        // 3. 게시 요건 검증 (모든 필드가 작성되어야 함)
        validateForPublishEntity(post);

        // 4. 재게시
        post.setType(post.getType() == null ? Type.PROMOTION : post.getType());
        post.setPublishedAt(LocalDateTime.now());

        Post saved = postRepository.save(post);
        return buildPublishResponse(saved);
    }

    // 제휴 게시글 삭제하기
    @Transactional
    public PromotionDeleteResponse delete(Long postId) {

        Post post = postRepository.findById(postId).orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 게시된 글만 삭제 가능
        if (post.getStatus() != Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus.CANNOT_DELETE_POST);
        }

        post.setStatus(Status.DELETED);
        postRepository.save(post);

        return new PromotionDeleteResponse(
                post.getPostId(),
                Type.RECRUITING,
                post.getStatus()

        );
    }

    // 모든 사용자가 작성한 제휴 홍보 게시글 카드뷰 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getAllPromotionList(OrderBy orderBy, List<Category> categories) {

        Status status = Status.PUBLISHED;
        List<Type> types = List.of(Type.BENEFIT, Type.PROMOTION);

        Predicate<Post> categoryFilter = (categories == null || categories.isEmpty())
                ? null
                : post -> post.getCategory() != null && categories.contains(post.getCategory());

        return postService.getAllPostList(types, status, orderBy, categoryFilter);

    }

    // 내가 작성한 제휴 홍보 게시글 카드뷰 조회
    @Transactional(readOnly = true)
    public List<PromotionListResponse> getMyPromotionsList(Long userId, Type type) {
        List<Status> statuses = List.of(Status.DRAFT, Status.PUBLISHED);

        return postRepository.findByUser_UserIdAndTypeAndStatusIn(userId, type, statuses)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    // 게시된 제휴 홍보글 단건 조회
    @Transactional(readOnly = true)
    public PromotionDetailResponse getPromotionDetail(Long userId, Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId).orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // type이 PROMOTION인지 검증
        if (post.getType() != Type.PROMOTION) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "해당 게시글은 제휴 홍보글이 아닙니다.");
        }

        // Post 엔티티에 연결된 Agreement 엔티티 가져오기
        Agreement agreement = post.getAgreement();
        if (agreement == null) {
            // 제휴 협약서가 없는 경우
            throw new GeneralException(ErrorStatus.NOT_FOUND, "해당 게시글에 연결된 협약서가 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 발신자 정보 처리
        Object senderInfo = null;
        if (Boolean.TRUE.equals(post.getExposeProposerInfo())) {
            User sender = agreement.getSender();
            if (sender != null) {
                senderInfo = postService.buildAuthorSummary(sender);
            }
        }

        // 수신자 정보 처리
        Object receiverInfo = null;
        if (Boolean.TRUE.equals(post.getExposeTargetInfo())) {
            User receiver = agreement.getReceiver();
            if (receiver != null) {
                receiverInfo = postService.buildAuthorSummary(receiver);
            }
        }

        return PromotionDetailResponse.builder()
                .userId(userId)
                .category(post.getUser().getInterest().getDescription())
                .postId(post.getPostId())
                .type(post.getType())
                .status(post.getStatus())
                .exposureEndDate(post.getExposureEndDate())
                .bookmarkCount(post.getBookmarkCount())
                .postImage(post.getPostImage())
                .title(post.getTitle())
                .target(post.getTarget())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .benefit(post.getBenefit())
                .condition(post.getCondition())
                .content(post.getContent())
                .sender(senderInfo)
                .receiver(receiverInfo)
                .build();
    }

    // agreement 본문 내용에서 데이터 파싱
    private Map<String, String> parseAgreementContent(String content) {
        Map<String, String> data = new HashMap<>();

        if (!hasText(content)) {
            return data;
        }

        // 대상 (ex. "혜택 제공 대상은 성신여대 재학생 및 교직원으로 한정한다.")
        Pattern targetPattern = Pattern.compile("혜택 제공 대상은 (.+?)으로 한정한다", Pattern.DOTALL);
        Matcher targetMatcher = targetPattern.matcher(content);
        if (targetMatcher.find()) {
            data.put("target", targetMatcher.group(1).trim());
        } else {
            data.put("target", "혜택 대상 정보 없음");
        }

        // 기간 (ex. "혜택 제공 기간은 2025년 9월 1일부터 2025년 9월 14일까지로 한다.")
        Pattern periodPattern = Pattern.compile("혜택 제공 기간은 (.+?)까지로 한다", Pattern.DOTALL);
        Matcher periodMatcher = periodPattern.matcher(content);
        if (periodMatcher.find()) {
            data.put("period", periodMatcher.group(1).trim());
        } else {
            data.put("period", "기간 정보 없음");
        }

        // 혜택 (ex. "결제 금액의 10% 할인 제공")
        // "다음과 같은 혜택을 제공한다" 섹션 안에서 `-`로 시작하는 항목을 찾음
        Pattern benefitPattern = Pattern.compile("다음과 같은 혜택을 제공한다:\\s*-(.+?)-", Pattern.DOTALL);
        Matcher benefitMatcher = benefitPattern.matcher(content);
        if (benefitMatcher.find()) {
            data.put("benefit", benefitMatcher.group(1).trim());
        } else {
            data.put("benefit", "혜택 정보 없음");
        }

        // 조건 (ex. "학생증 또는 교직원증 제시 필수")
        Pattern conditionPattern = Pattern.compile("혜택 이용 시 (.+?) 필수", Pattern.DOTALL);
        Matcher conditionMatcher = conditionPattern.matcher(content);
        if (conditionMatcher.find()) {
            data.put("condition", conditionMatcher.group(1).trim());
        } else {
            data.put("condition", "조건 정보 없음");
        }

        return data;
    }

    // 프롬프트 생성
    private String buildPrompt(Map<String, String> agreementData, Post post) {
        String template = """
            아래 정보를 바탕으로 홍보 게시글을 350자-500자 내외로 작성
          
                ## 계약 정보
                * **제휴 대상**: {target}
                * **기간**: {period}
                * **혜택**: {benefit}
                * **조건**: {condition}
            
                ## 게시글 정보
                * **제목**: {postTitle}
                * **내용**: {postContent}
            
                ## 작성 조건
                * **다음과 같은 섹션을 포함할 것:**
                    * 💚 대상
                    * 💚 혜택
                    * 💚 기간
                * **핵심 내용(대상, 혜택, 기간, 조건)은 목록 형태로 명확하게 정리**
                * **마지막 문단에는 마무리 문구를 1-2문장 사용**
                * 게시글의 내용과 관련된 이모티콘을 적절히 사용
                * 응답은 JSON 형식으로 반환: {"title": "...", "content": "..."}
            """;

        return template.replace("{target}", agreementData.getOrDefault("target", ""))
                .replace("{period}", agreementData.getOrDefault("period", "기간 미정"))
                .replace("{benefit}", agreementData.getOrDefault("benefit", "혜택 미정"))
                .replace("{condition}", agreementData.getOrDefault("condition", "조건 미정"))
                .replace("{postTitle}", post.getTitle())
                .replace("{postContent}", post.getContent());
    }

    private String callGemini(String prompt) {
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

            return text.trim();

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, e.getMessage());

        }
    }

    private PromotionManualPublishResponse buildPublishResponse(Post saved) {
        return PromotionManualPublishResponse.builder()
                .type(saved.getType())
                .status(saved.getStatus())
                .postId(saved.getPostId())
                .senderName(saved.getSender().getProfileName())
                .receiverName(saved.getReceiver().getProfileName())
                .postImage(saved.getPostImage())
                .title(saved.getTitle())
                .target(saved.getTarget())
                .benefit(saved.getBenefit())
                .condition(saved.getCondition())
                .content(saved.getContent())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .exposureEndDate(saved.getExposureEndDate())
                .exposeProposerInfo(saved.getExposeProposerInfo())
                .exposeTargetInfo(saved.getExposeTargetInfo())
                .publishedAt(saved.getPublishedAt())
                .build();
    }

    // 필수값 검증
    private void validateForPublish(PromotionManualPublishRequest request) {
        if (!hasText(request.getTitle())
            || !hasText(request.getTarget())
            || !hasText(request.getBenefit())
            || !hasText(request.getCondition())
            || !hasText(request.getContent())
            || request.getStartDate() == null || request.getEndDate() == null
            || request.getExposureEndDate() == null ){

            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }

        // 이미지 필수 검증
        if (request.getPostImage() == null || request.getPostImage().isEmpty()) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "이미지는 필수입니다.");
        }

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (end.isBefore(start)) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID);
        }
    }

    // 게시를 위한 필수값 검증 (모든 필드가 작성돼 있는지 확인)
    private void validateForPublishEntity(Post p) {
        if (!hasText(p.getTitle()) || !hasText(p.getTarget())
                || !hasText(p.getBenefit()) || !hasText(p.getCondition())
                || !hasText(p.getContent())
                || p.getStartDate() == null || p.getEndDate() == null
                || p.getExposureEndDate() == null) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }
        if (p.getEndDate().isBefore(p.getStartDate())) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID);
        }
    }

    // 임시 저장을 위해서는 최소 1개 이상의 필드가 작성돼야 함
    private void validateHasAnyDraftField(PromotionDraftSaveRequest request) {
        boolean hasAny =
                request.getPostImage() != null && !request.getPostImage().isEmpty() ||
                        hasText(request.getTitle()) ||
                        hasText(request.getTarget())||
                request.getStartDate() != null || request.getEndDate() != null ||
                hasText(request.getBenefit()) || hasText(request.getCondition()) ||
                hasText(request.getContent()) || request.getExposureEndDate() != null ||
                Boolean.TRUE.equals(request.getExposeProposerInfo()) ||
                Boolean.TRUE.equals(request.getExposeTargetInfo());

        if (!hasAny) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "임시 저장을 위해서는 1개 이상의 필드가 필요합니다.");
        }

    }

    // 수정 사항 반영 공통 코드
    private void applyPatchCore(
            Post post,
            String title, String target,
            LocalDate startDate, LocalDate endDate,
            String benefit, String condition, String content,
            LocalDate exposureEndDate,
            Boolean exposeProposerInfo, Boolean exposeTargetInfo
    ) {
        if (hasText(title)) post.setTitle(title);
        if (hasText(target)) post.setTarget(target);
        if (hasText(benefit)) post.setBenefit(benefit);
        if (hasText(condition)) post.setCondition(condition);
        if (hasText(content)) post.setContent(content);

        if (startDate != null) post.setStartDate(startDate);
        if (endDate != null) post.setEndDate(endDate);
        if (exposureEndDate != null) post.setExposureEndDate(exposureEndDate);

        if (exposeProposerInfo != null) post.setExposeProposerInfo(exposeProposerInfo);
        if (exposeTargetInfo != null) post.setExposeTargetInfo(exposeTargetInfo);
    }

    // draft DTO
    private void applyPatch(Post post, PromotionDraftSaveRequest r) {
        applyPatchCore(post,
                r.getTitle(), r.getTarget(),
                r.getStartDate(), r.getEndDate(),
                r.getBenefit(), r.getCondition(), r.getContent(),
                r.getExposureEndDate(),
                r.getExposeProposerInfo(), r.getExposeTargetInfo());
    }

    // 재수정 즉시 게시 DTO
    private void applyPatch(Post post, PromotionManualPublishRequest r) {
        applyPatchCore(post,
                r.getTitle(), r.getTarget(),
                r.getStartDate(), r.getEndDate(),
                r.getBenefit(), r.getCondition(), r.getContent(),
                r.getExposureEndDate(),
                r.getExposeProposerInfo(), r.getExposeTargetInfo());
    }

    private void validateOptionalDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID, "종료일이 시작일보다 빠릅니다.");
        }
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

    private PromotionListResponse toListResponse(Post post) {
        return PromotionListResponse.builder()
                .postId(post.getPostId())
//                .userId(post.getUserId())
                .userId(post.getUser().getUserId())
                .type(post.getType())
                .status(post.getStatus())
                .bookmarkCount(post.getBookmarkCount())
                .exposureEndDate(post.getExposureEndDate())
                .postImage(post.getPostImage())
                .title(post.getTitle())
                .target(post.getTarget())
                .startDate(post.getStartDate())
                .endDate(post.getEndDate())
                .benefit(post.getBenefit())
                .build();
    }

    // 모델이 종종 붙이는 ```json ... ``` / ``` ... ``` / 앞뒤 설명문 제거 및 따옴표/BOM 정규화
    private String sanitizeModelTextToJson(String text) {
        if (text == null) return "";
        String s = text.trim();

        // 코드펜스(백틱) 제거
        s = s.replaceAll("(?s)^```(?:json)?\\s*", "");
        s = s.replaceAll("(?s)\\s*```$", "");

        // 최초 '{'부터 마지막 '}'까지 추출 (앞뒤 잡설 제거)
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            s = s.substring(start, end + 1);
        }

        // 스마트 따옴표 정규화
        s = s.replace('“','"').replace('”','"')
                .replace('’','\'').replace('‘','\'');

        // BOM 제거
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.trim();
    }

    // JSON 파싱: 1차 엄격 시도 → 실패 시 sanitize 후 2차 시도
    private JsonNode parseJsonStrictFirstThenLenient(ObjectMapper mapper, String raw) {
        try {
            return mapper.readTree(raw);
        } catch (IOException first) {
            String cleaned = sanitizeModelTextToJson(raw);
            try {
                return mapper.readTree(cleaned);
            } catch (IOException second) {
                String preview = cleaned.length() > 200 ? cleaned.substring(0, 200) + "..." : cleaned;
                throw new GeneralException(
                        ErrorStatus.INTERNAL_ERROR,
                        "AI 응답 파싱 실패 (preview=" + preview + ")",
                        second
                );
            }
        }
    }

    private static class DateRange {
        final LocalDate start;
        final LocalDate end;
        DateRange(LocalDate s, LocalDate e) { this.start = s; this.end = e; }
    }

    private DateRange parseDateRangeFlexible(String text, LocalDate base) {
        if (text == null) text = "";
        // 물결/대시/스페이스 정규화
        String t = text.replaceAll("[~∼〜－–—]", "~")
                .replaceAll("\\s+", " ")
                .trim();

        // 1) 완전한 한국어 날짜 2개: 2025년 9월 1일 ~ 2025년 9월 14일
        Pattern FULL_KO2 = Pattern.compile(
                "(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*~\\s*(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일"
        );

        // 2) 앞은 연·월·일, 뒤는 '일'만: 2025년 9월 1일 ~ 14일
        Pattern KO_DAY_OMITTED = Pattern.compile(
                "(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*~\\s*(\\d{1,2})\\s*일"
        );

        // 3) 앞은 연·월·일, 뒤는 월·일: 2025년 9월 1일 ~ 10월 14일 (연 생략)
        Pattern KO_YEAR_OMITTED = Pattern.compile(
                "(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*~\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일"
        );

        // 4) 단순 KO 범위: 9월 1일~14일 (연 생략, base에서 연도 보정)
        Pattern KO_SIMPLE = Pattern.compile(
                "(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*~\\s*(\\d{1,2})(?:\\s*월)?\\s*(\\d{1,2})\\s*일"
        );

        // 5) ISO/숫자 혼합: 2025-09-01 ~ 2025-09-14
        Pattern ISO2 = Pattern.compile(
                "(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})\\s*~\\s*(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})"
        );

        // 6) 앞은 Y-M-D, 뒤는 M-D만: 2025-09-01 ~ 10-14
        Pattern ISO_YEAR_OMITTED = Pattern.compile(
                "(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})\\s*~\\s*(\\d{1,2})[-./](\\d{1,2})"
        );

        // 7) MM/DD ~ MM/DD (연도 생략 → base에서 보정)
        Pattern MD_RANGE = Pattern.compile(
                "(\\d{1,2})[-./](\\d{1,2})\\s*~\\s*(\\d{1,2})[-./](\\d{1,2})"
        );

        // 8) 단일 날짜만 있는 경우: 단일일 프로모션으로 간주(요구사항에 맞게 선택)
        Pattern SINGLE_KO = Pattern.compile("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
        Pattern SINGLE_ISO = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})");

        LocalDate todayBase = (base != null) ? base : LocalDate.now();

        Matcher m;

        // 1
        m = FULL_KO2.matcher(t);
        if (m.find()) {
            LocalDate s = LocalDate.of(i(m,1), i(m,2), i(m,3));
            LocalDate e = LocalDate.of(i(m,4), i(m,5), i(m,6));
            return ordered(s, e);
        }
        // 2
        m = KO_DAY_OMITTED.matcher(t);
        if (m.find()) {
            int y=i(m,1), mo=i(m,2), d1=i(m,3), d2=i(m,4);
            LocalDate s = LocalDate.of(y, mo, d1);
            LocalDate e = LocalDate.of(y, mo, d2);
            return ordered(s, e);
        }
        // 3
        m = KO_YEAR_OMITTED.matcher(t);
        if (m.find()) {
            int y=i(m,1), m1=i(m,2), d1=i(m,3), m2=i(m,4), d2=i(m,5);
            LocalDate s = LocalDate.of(y, m1, d1);
            LocalDate e = LocalDate.of(y, m2, d2);
            // 연도 넘어가는 케이스 보정: 12월~1월이면 +1년
            if (m2 < m1) e = e.plusYears(1);
            return ordered(s, e);
        }
        // 4
        m = KO_SIMPLE.matcher(t);
        if (m.find()) {
            int m1=i(m,1), d1=i(m,2), m2=i(m,3), d2=i(m,4);
            LocalDate s = LocalDate.of(todayBase.getYear(), m1, d1);
            LocalDate e = LocalDate.of(todayBase.getYear(), (m2==0?m1:m2), d2);
            if ((m2!=0) && m2 < m1) e = e.plusYears(1);
            return ordered(s, e);
        }
        // 5
        m = ISO2.matcher(t);
        if (m.find()) {
            LocalDate s = LocalDate.of(i(m,1), i(m,2), i(m,3));
            LocalDate e = LocalDate.of(i(m,4), i(m,5), i(m,6));
            return ordered(s, e);
        }
        // 6
        m = ISO_YEAR_OMITTED.matcher(t);
        if (m.find()) {
            int y=i(m,1), m1=i(m,2), d1=i(m,3), m2=i(m,4), d2=i(m,5);
            LocalDate s = LocalDate.of(y, m1, d1);
            LocalDate e = LocalDate.of(y, m2, d2);
            if (m2 < m1) e = e.plusYears(1);
            return ordered(s, e);
        }
        // 7
        m = MD_RANGE.matcher(t);
        if (m.find()) {
            int m1=i(m,1), d1=i(m,2), m2=i(m,3), d2=i(m,4);
            LocalDate s = LocalDate.of(todayBase.getYear(), m1, d1);
            LocalDate e = LocalDate.of(todayBase.getYear(), m2, d2);
            if (m2 < m1) e = e.plusYears(1);
            return ordered(s, e);
        }
        // 8) 단일 날짜만 발견 시(요구에 따라 한-day 처리 or 에러)
        m = SINGLE_KO.matcher(t);
        if (m.find()) {
            LocalDate s = LocalDate.of(i(m,1), i(m,2), i(m,3));
            return new DateRange(s, s);
        }
        m = SINGLE_ISO.matcher(t);
        if (m.find()) {
            LocalDate s = LocalDate.of(i(m,1), i(m,2), i(m,3));
            return new DateRange(s, s);
        }

        throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "기간에서 날짜 2개를 찾지 못했습니다.");
    }

    private static int i(Matcher m, int g) { return Integer.parseInt(m.group(g)); }
    private static DateRange ordered(LocalDate a, LocalDate b) {
        return b.isBefore(a) ? new DateRange(b, a) : new DateRange(a, b);
    }

}
