package com.itzi.itzi.posts.service;

import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.OrgType;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.dto.response.*;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.posts.dto.request.PostDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.AuthorSummaryResponse;
import com.itzi.itzi.recruitings.dto.response.StoreSummaryResponse;
import com.itzi.itzi.store.domain.Store;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // 제휴 홍보글 삭제하기
    @Transactional
    public PostDeleteResponse deletePost(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 게시된 홍보글만 삭제 가능
        if (post.getStatus() == Status.DELETED || post.getStatus() == Status.DRAFT) {
            throw new GeneralException(ErrorStatus.CANNOT_DELETE_POST);
        }

        post.setStatus(Status.DELETED);
        postRepository.save(post);

        return new PostDeleteResponse(
                post.getType(),
                post.getPostId(),
                post.getStatus()
        );
    }

    // 제휴 홍보글 게시하기
    @Transactional
    public PostPublishResponse pusblishPost(Long postId) {

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
        post.setPublishedAt(LocalDateTime.now());

        postRepository.save(post);

        return new PostPublishResponse(
                post.getType(),
                post.getPostId(),
                post.getStatus(),
                post.getPublishedAt()
        );
    }

    @Transactional
    public PostDraftSaveResponse saveOrUpdateDraft(Long userId, Type type, PostDraftSaveRequest request) {

        // 임시 저장을 하기 위해서는 최소 1개 이상의 필드가 작성돼 있어야 함
        validateHasAnyDraftField(request);

        if (type != Type.RECRUITING && type != Type.BENEFIT) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "유효하지 않은 게시물 타입입니다.");
        }

        Post post;
        if (request.getPostId() != null) {
            // 기존 게시물 업데이트
            post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND, "존재하지 않는 postId입니다."));

            // status가 PUBLISHED, DELETED일 경우 임시 저장 불가
            if (post.getStatus() != Status.DRAFT) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST, "임시 저장은 DRAFT 상태의 게시글에만 가능합니다.");
            }

            // 2) 부분 업데이트(널이면 무시, 값이 있으면 반영)
            applyPatch(post, request);
            handleImageUpload(post, request.getPostImage());

        } else {

            // 1. userId로 User 엔티티 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

            // 새 DRAFT 글 생성
            post = Post.builder()
                    .user(user)
                    .status(Status.DRAFT)
                    .type(type)
                    .bookmarkCount(0L)
                    .build();
            applyPatch(post, request);
            handleImageUpload(post, request.getPostImage());
        }

        Post saved = postRepository.save(post);

        return new PostDraftSaveResponse(
                saved.getType(),
                saved.getPostId(),
                userId,
                saved.getStatus(),
                saved.getUpdatedAt()
        );
    }

    // 공통 게시글 단건 상세 조회
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId, Type type) {

        Post post = postRepository.findByPostIdAndType(postId, type)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        User author = post.getUser();
        Object authorSummary = buildAuthorSummary(author);

        return PostDetailResponse.builder()
                .userId(author.getUserId())
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
                .author(authorSummary)
                .build();
    }

    // 공통: 내가 작성한 게시글 전체 리스트 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getMyPostList(Long userId, List<Status> statuses, Type type) {
        return postRepository.findByUser_UserIdAndTypeAndStatusIn(userId, type, statuses)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    // 공통: 모든 사용자가 작성한 게시글 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getAllPostList(List<Type> types, Status status, OrderBy orderBy, @Nullable Predicate<Post> filter) {

        List<Post> posts = new ArrayList<>();

        // 기본 정렬 기준: 마감 임박순
        if (orderBy == null) {
            orderBy = OrderBy.CLOSING;
        }

        switch (orderBy) {
            case CLOSING -> {
                LocalDate today = LocalDate.now();
                posts = postRepository.findByTypeInAndStatusAndExposureEndDateGreaterThanEqual(
                        types, status, today, Sort.by(Sort.Direction.ASC, "exposureEndDate")
                );
            }

            case POPULAR -> {
                posts = postRepository.findByTypeInAndStatus(
                        types, status, Sort.by(Sort.Direction.DESC, "bookmarkCount"));
            }

            case LATEST -> {
                posts = postRepository.findByTypeInAndStatus(
                        types, status, Sort.by(Sort.Direction.DESC, "publishedAt"));
            }

            case OLDEST -> {
                posts = postRepository.findByTypeInAndStatus(
                        types, status, Sort.by(Sort.Direction.ASC, "publishedAt"));
            }
        }

        // 필터링 (기본값: 전체 조회)
        if (filter != null) {
            posts = posts.stream().filter(filter).toList();
        }

        return posts.stream().map(this::toListResponse).toList();
    }

    // 작성자 정보 요약 생성
    public Object buildAuthorSummary(User author) {
        if (author.getOrgProfile() != null && author.getOrgProfile().getOrgType() == OrgType.STORE) {
            return buildStoreSummary(author.getStore());
        } else {
            return buildAuthorSummary(author, author.getOrgProfile());
        }
    }


    // 임시 저장을 하기 위해서는 최소 1개 이상의 필드가 작성돼 있어야 함
    private void validateHasAnyDraftField(PostDraftSaveRequest request) {
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
    private void applyPatch(Post e, PostDraftSaveRequest request) {
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

    // 이미지 업로드/변경
    public void handleImageUpload(Post entity, MultipartFile file) {
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

    private PostListResponse toListResponse(Post post) {
        return PostListResponse.builder()
                .postId(post.getPostId())
                .userId(post.getUser().getUserId())
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

    // 일반 작성자/조직의 요약 정보 생성
    private AuthorSummaryResponse buildAuthorSummary(User author, OrgProfile org) {
        return AuthorSummaryResponse.builder()
                .userId(author.getUserId())
                .image(author.getProfileImage())
                .rating(author.getOrgProfile().getRating())
                .name(author.getProfileName())
                .info(org != null ? org.getIntro() : null)
                .keywords(org != null ? org.getKeywords() : null)
                .schoolName(org.getSchoolName())
                .unitName(org.getUnitName())
                .phone(org != null ? org.getPhone() : null)
                .address(org != null ? org.getAddress() : null)
                .ownerName(org != null ? org.getOwnerName() : null)
                .linkUrl(org != null ? org.getLinkUrl() : null)
                .build();
    }

    // 상점의 요약 정보 생성
    private StoreSummaryResponse buildStoreSummary(Store store) {
        return StoreSummaryResponse.builder()
                .userId(store.getUser().getUserId())
                .image(store.getStoreImage())
                .rating(store.getRating())
                .name(store.getName())
                .info(store.getInfo())
                .keywords(store.getKeywords())
                .category(store.getCategory().getDescription())
                .operatingHours(store.getOperatingHours())
                .phone(store.getPhone())
                .address(store.getAddress())
                .ownerName(store.getOwnerName())
                .linkUrl(store.getLinkUrl())
                .build();
    }
}
