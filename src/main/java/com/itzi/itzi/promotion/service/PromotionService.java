package com.itzi.itzi.promotion.service;

import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.promotion.dto.request.PromotionDraftSaveRequest;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.PromotionDraftSaveResponse;
import com.itzi.itzi.promotion.dto.response.PromotionEditViewResponse;
import com.itzi.itzi.promotion.dto.response.PromotionManualPublishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.util.StringUtils.hasText;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {

    private final PostRepository postRepository;
    private final S3Service s3Service;

    // 제휴 게시글 수동 작성 후 업로드
    @Transactional
    public PromotionManualPublishResponse promotionManualPublish(Long userId, PromotionManualPublishRequest request) {

        // 0. 제휴 게시글을 맺을 수 있는 상태인지 검증 추가 필요

        // 1. 필수 값 검증
        validateForPublish(request);

        // 2. 엔티티 생성 및 저장
        Post post = Post.builder()
                .type(Type.PROMOTION)
                .status(Status.PUBLISHED)
                .userId(userId)
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
                .publishedAt(LocalDateTime.now())
                .build();

        // 3. 이미지 업로드
        handleImageUpload(post, request.getPostImage());

        postRepository.save(post);

        // 4. 응답 반환
        return PromotionManualPublishResponse.builder()
                .type(post.getType())
                .status(post.getStatus())
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
                .publishedAt(post.getPublishedAt())
                .build();

    }

    // 제휴 게시글 임시 저장
    @Transactional
    public PromotionDraftSaveResponse promotionDraft(Long userId, PromotionDraftSaveRequest request) {

        // 0. 제휴 게시글을 맺을 수 있는 상태인지 검증 추가 필요

        // 1. 필수 값 검증
        validateHasAnyDraftField(request);

        // 2. 날짜 역전 검증
        validateOptionalDateRange(request.getStartDate(), request.getEndDate());

        Post post;

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
                    .userId(userId)
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

    private PromotionManualPublishResponse buildPublishResponse(Post saved) {
        return PromotionManualPublishResponse.builder()
                .type(saved.getType())
                .status(saved.getStatus())
                .postId(saved.getPostId())
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

    // 필수값 검증 (재게시 용)
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

}
