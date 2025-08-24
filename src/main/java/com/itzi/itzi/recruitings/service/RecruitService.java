package com.itzi.itzi.recruitings.service;

import com.itzi.itzi.auth.domain.OrgType;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.global.gemini.GeminiService;
import com.itzi.itzi.posts.dto.response.*;
import com.itzi.itzi.posts.service.PostService;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.posts.dto.request.PostDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final GeminiService geminiService;
    private final PostService postService;

    public RecruitingAiGenerateResponse generateRecruitingAi(Long userId, Type type, RecruitingAiGenerateRequest request) {

        // 1. 검증 : 날짜 역전 금지, 모든 필드 작성
        validate(type, request);

        // 2. 프롬프트 구성
        String prompt = buildPrompt(type, request);

        // 3. Gemini 호출 (GeminiService 사용)
        String content = geminiService.callGemini(prompt);

        // 4. 엔티티 구성
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + userId));

        Post entity = Post.builder()
                        .user(user)
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

        // 7. 응답 DTO
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

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String buildPrompt(Type type, RecruitingAiGenerateRequest r) {

        // MultipartFile 기준으로 존재 여부만 판단
        String postImageLine = (r.getPostImage() != null && !r.getPostImage().isEmpty())
                ? "\n(이미지 첨부됨)"
                : "";

        // 제목, 타깃에서 학교명 자동 추출
        String school = geminiService.extractSchoolName(r.getTitle())
                .or(() -> geminiService.extractSchoolName(r.getTarget()))
                .orElse("00대학교");       // 기본값

        // 대상, 기간, 혜택, 조건 협의 가능 문구
        boolean targetOk  = Boolean.TRUE.equals(r.getTargetNegotiable());
        boolean periodOk  = Boolean.TRUE.equals(r.getPeriodNegotiable());
        boolean benefitOk = Boolean.TRUE.equals(r.getBenefitNegotiable());
        boolean condOk    = Boolean.TRUE.equals(r.getConditionNegotiable());
        String periodCondNote = (targetOk || periodOk || benefitOk || condOk ) ? " (대상, 기간, 혜택, 조건 협의 가능)" : "";

        return """
        너는 아래 '원본 텍스트'를 참고하여, 명시된 규칙에 따라 빈칸을 채우거나 일부 문구를 수정하여 최종 본문만 출력한다.
        - 이모지 사용 규칙
            1) 이모지 리스트 `[☺️😊😚🙌🏻🤝🏻🤙🏻🙏🏻🍀⭐️💌📍❗️️💬📢🕒]`에서 **3개를 무작위로 선택**
            2) **1문단과 2문단 본문 내용에서만** 적절히 배치
            3) 이모지는 문장 중간에 삽입 금지
            4) 📅 제휴 기간, 🎯 제휴 대상, 💬 문의 안내 **섹션과 해당 섹션의 본문에는 이모지 사용 금지**
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

        [원본 텍스트]
        [%s]

        안녕하십니까, %s입니다.
        저희는 %s 동안, %s분들께 혜택을 제공해 주실 상권 제휴 매장을 모집하고 있습니다.

        이번 제휴는 지역 상권과 학교 구성원 간의 상생과 교류를 목적으로 하며,
        제휴를 맺어주시는 매장에는 적극적인 홍보를 통해 방문 유도와 인지도 향상을 도와드릴 예정입니다.
        제휴 혜택의 형태는 매장 상황과 특성에 맞추어 자유롭게 설정하실 수 있으며,
        할인, 쿠폰 제공, 사은품 증정, 시즌 이벤트 등 다양한 방식으로 협의가 가능합니다.

        📅 제휴 기간
        %s ~ %s

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

    // 제휴 모집글 임시 저장
    @Transactional
    public PostDraftSaveResponse
    saveOrUpdateDraft(Long userId, Type type, PostDraftSaveRequest request) {

        if (type != Type.RECRUITING) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "RECRUITING 타입의 게시물만 임시 저장할 수 있습니다.");
        }

        return postService.saveOrUpdateDraft(userId, type, request);
    }


    // 제휴 모집글 게시하기
    @Transactional
    public PostPublishResponse publishRecruiting(Long postId) {

        // 1. 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 2. 게시글 타입 검증: RECRUITING 타입만 업로드 가능하도록
        if (post.getType() != Type.RECRUITING) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "RECRUITING 타입의 게시물만 업로드할 수 있습니다.");
        }

        return postService.pusblishPost(postId);
    }

    // 제휴 모집글 삭제하기
    @Transactional
    public PostDeleteResponse deleteRecruiting(Long postId) {

        // 1. 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 2. 게시글 타입 검증: RECRUITING 타입만 삭제 가능하도록
        if (post.getType() != Type.RECRUITING) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "RECRUITING 타입의 게시물만 삭제할 수 있습니다.");
        }

        return postService.deletePost(postId);
    }

    // 작성한 게시글 단건 상세 내용 조회
    @Transactional(readOnly = true)
    public PostDetailResponse getRecruitingDetail(Long postId) {
        return postService.getPostDetail(postId, Type.RECRUITING);
    }

    // 내가 작성한 게시글 전체 리스트 조회 (userId = 1 고정)
    @Transactional(readOnly = true)
    public List<PostListResponse> getMyRecruitingList() {
        Long FIXED_USER_ID = 1L;
        List<Status> statuses = List.of(Status.DRAFT, Status.PUBLISHED);

        return postService.getMyPostList(FIXED_USER_ID, statuses, Type.RECRUITING);

    }

    // 모든 사용자가 작성한 제휴 모집글 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getAllRecruitingList(OrderBy orderBy, List<String> filters) {
        Status status = Status.PUBLISHED;
        List<Type> types = List.of(Type.RECRUITING);

        Predicate<Post> recruitingFilter = (filters == null || filters.isEmpty())
                ? null
                : post -> {
            String benefit = post.getBenefit();
            return benefit != null && filters.stream().anyMatch(benefit::contains);
        };

        return postService.getAllPostList(types, status, orderBy, recruitingFilter);
    }

    // org_type 값에 따라 게시글 필터링
    @Transactional(readOnly = true)
    public Page<PostListResponse> getPostsByOrgType(String orgType, Pageable pageable) {
        Page<Post> posts;

        if ("전체".equalsIgnoreCase(orgType)) {
            posts = postRepository.findAll(pageable);
        } else {
            // orgType 매핑 처리
            OrgType type;
            try {
                type = OrgType.valueOf(orgType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Page.empty(pageable); // 잘못된 값이면 빈 페이지 반환
            }
            posts = postRepository.findByUser_OrgProfile_OrgType(type, pageable);
        }

        // Post → PostListResponse 매핑
        return posts.map(post -> PostListResponse.builder()
                .postId(post.getPostId())
                .userId(post.getUser().getUserId())
                .category(post.getCategory())
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
                .build());
    }
}