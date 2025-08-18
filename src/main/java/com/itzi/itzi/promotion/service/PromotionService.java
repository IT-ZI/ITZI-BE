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

    // 작성한 부분만 업데이트
    private void applyPatch(Post post, PromotionDraftSaveRequest request) {
        if (hasText(request.getTitle())) post.setTitle(request.getTitle());
        if (hasText(request.getTarget())) post.setTarget(request.getTarget());
        if (hasText(request.getBenefit())) post.setBenefit(request.getBenefit());
        if (hasText(request.getCondition())) post.setCondition(request.getCondition());
        if (hasText(request.getContent())) post.setContent(request.getContent());

        if (request.getStartDate() != null) post.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) post.setEndDate(request.getEndDate());
        if (request.getExposureEndDate() != null) post.setExposureEndDate(request.getExposureEndDate());

        if (request.getExposeProposerInfo() != null) post.setExposeProposerInfo(request.getExposeProposerInfo());
        if (request.getExposeTargetInfo() != null) post.setExposeTargetInfo(request.getExposeTargetInfo());
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
