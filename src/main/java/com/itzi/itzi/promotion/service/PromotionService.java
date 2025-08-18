package com.itzi.itzi.promotion.service;

import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.PromotionManualPublishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // 필수값 검증
    private void validateForPublish(PromotionManualPublishRequest request) {
        if (!StringUtils.hasText(request.getTitle())
            || !StringUtils.hasText(request.getTarget())
            || !StringUtils.hasText(request.getBenefit())
            || !StringUtils.hasText(request.getCondition())
            || !StringUtils.hasText(request.getContent())
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
