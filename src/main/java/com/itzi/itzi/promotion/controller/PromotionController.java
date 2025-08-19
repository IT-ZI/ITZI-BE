package com.itzi.itzi.promotion.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.promotion.dto.request.PromotionDraftSaveRequest;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.*;
import com.itzi.itzi.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/promotion")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    // 제휴 게시글 수동 작성 후 업로드
    @PostMapping
    public ApiResponse<PromotionManualPublishResponse> promotionManualPublish(
            @ModelAttribute PromotionManualPublishRequest request
    ){

        Long fixedUserId = 1L;                 // 항상 1
        PromotionManualPublishResponse response = promotionService.promotionManualPublish(fixedUserId, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 제휴 게시글 임시 저장
    @PostMapping("/draft")
    public ApiResponse<PromotionDraftSaveResponse> promotionDraft(
            @ModelAttribute PromotionDraftSaveRequest request
    ) {
        Long fixedUserId = 1L;
        PromotionDraftSaveResponse response = promotionService.promotionDraft(fixedUserId, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 제휴 게시글 업로드
    @PatchMapping("{postId}/publish")
    public ApiResponse<PromotionPublishResponse> publish(@PathVariable Long postId ) {
        PromotionPublishResponse response = promotionService.publish(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 재수정 진입 (작성된 글 조회)
    @GetMapping("/{postId}/edit")
    public ApiResponse<PromotionEditViewResponse> getEditView(
            @PathVariable Long postId) {
        PromotionEditViewResponse response = promotionService.getEditView(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 재수정 후 즉시 게시
    @PatchMapping("/{postId}/republish")
    public ApiResponse<PromotionManualPublishResponse> republish(
            @PathVariable Long postId,
            @ModelAttribute PromotionManualPublishRequest request
    ) {

        Long fixedUserId = 1L;                 // 항상 1
        PromotionManualPublishResponse response = promotionService.republish(fixedUserId, postId, request);

        return ApiResponse.of(SuccessStatus._OK, response);

    }

    // 게시물 삭제하기
    @DeleteMapping("/{postId}")
    public ApiResponse<PromotionDeleteResponse> delete(@PathVariable Long postId) {

        PromotionDeleteResponse response = promotionService.delete(postId);
        return ApiResponse.of(SuccessStatus._OK, response);

    }

}
