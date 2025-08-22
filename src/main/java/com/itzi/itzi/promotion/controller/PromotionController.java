package com.itzi.itzi.promotion.controller;

import com.itzi.itzi.agreement.domain.Docs;
import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.promotion.dto.request.PromotionDraftSaveRequest;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.*;
import com.itzi.itzi.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotion")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    // 제휴 홍보 게시글을 맺을 수 있는 제휴 대상자 리스트 조회
    @GetMapping("/available")
    public ApiResponse<List<String>> getAvailableDocs() {
        List<String> receiverNames  = promotionService.getAvailableDocs();
        return ApiResponse.of(SuccessStatus._OK, receiverNames);
    }

    // 제휴 게시글 수동 작성 후 업로드
    @PostMapping
    public ApiResponse<PromotionManualPublishResponse> promotionManualPublish(
            @RequestParam(name = "docsId") Long docsId,
            @ModelAttribute PromotionManualPublishRequest request
    ){

        PromotionManualPublishResponse response = promotionService.promotionManualPublish(docsId, request);

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

    // 모든 사용자가 작성한 제휴 홍보 게시글 목록 조회
    @GetMapping("/all")
    public ApiResponse<List<PromotionListResponse>> getAllPromotionList(
            @RequestParam Type type,
            @RequestParam(defaultValue = "CLOSING") OrderBy orderBy
    ) {
        List<PromotionListResponse> response = promotionService.getAllPromotionList(type, orderBy);
        return ApiResponse.of(SuccessStatus._OK, response);

    }

    // 내가 작성한 제휴 홍보 게시글 목록 조회
    @GetMapping("/mine")
    public ApiResponse<List<PromotionListResponse>> getMyPromotionsList(@RequestParam Type type) {

        List<PromotionListResponse> response = promotionService.getMyPromotionsList(type);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 게시된 제휴 홍보글 단건 조회
    @GetMapping("/{postId}")
    public ApiResponse<PromotionDetailResponse> getPromotionDetail(@PathVariable Long postId) {
        PromotionDetailResponse response = promotionService.getPromotionDetail(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }
}