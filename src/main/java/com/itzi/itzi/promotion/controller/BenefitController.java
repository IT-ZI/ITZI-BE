package com.itzi.itzi.promotion.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.dto.request.PostDraftSaveRequest;
import com.itzi.itzi.posts.dto.response.PostDeleteResponse;
import com.itzi.itzi.posts.dto.response.PostDraftSaveResponse;
import com.itzi.itzi.posts.dto.response.PostPublishResponse;
import com.itzi.itzi.promotion.dto.request.BenefitGenerateAiRequest;
import com.itzi.itzi.promotion.dto.response.BenefitGenerateAiResponse;
import com.itzi.itzi.promotion.service.BenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/benefit")
@RequiredArgsConstructor
@Slf4j
public class BenefitController {

    private final BenefitService benefitService;

    @PostMapping("/ai")
    public ApiResponse<BenefitGenerateAiResponse> generateBenefitAi(
            @ModelAttribute BenefitGenerateAiRequest request
    ) {
        Long fixedUserId = 1L;                 // 항상 1

        BenefitGenerateAiResponse response =
                benefitService.generateBenefitAi(fixedUserId, Type.BENEFIT, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 임시 저장
    @PostMapping("/draft")
    public ApiResponse<PostDraftSaveResponse> saveOrUpdateDraft(
            @ModelAttribute PostDraftSaveRequest request
    ) {
        Long fixedUserId = 1L;
        Type fixedType = Type.BENEFIT;

        PostDraftSaveResponse response =
                benefitService.saveOrUpdateDraft(fixedUserId, fixedType, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 게시하기
    @PatchMapping("/{postId}/publish")
    public ApiResponse<PostPublishResponse> publishBenefit(@PathVariable Long postId) {
        PostPublishResponse response = benefitService.publishBenefit(postId);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 삭제하기
    @DeleteMapping("/{postId}")
    public ApiResponse<PostDeleteResponse> deleteBenefit(@PathVariable Long postId) {
        PostDeleteResponse response = benefitService.deleteBenefit(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

}
