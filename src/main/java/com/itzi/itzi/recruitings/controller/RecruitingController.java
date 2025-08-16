package com.itzi.itzi.recruitings.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.recruitings.dto.request.RecruitingDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.RecruitingAiGenerateResponse;
import com.itzi.itzi.recruitings.dto.response.RecruitingDeleteResponse;
import com.itzi.itzi.recruitings.dto.response.RecruitingDraftSaveResponse;
import com.itzi.itzi.recruitings.dto.response.RecruitingPublishResponse;
import com.itzi.itzi.recruitings.service.RecruitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recruiting")
@RequiredArgsConstructor
@Slf4j
public class RecruitingController {

    private final RecruitService recruitService;

    // 작성된 기본 정보를 바탕으로 상세 내용 생성
    @PostMapping("/ai")
    public ApiResponse<RecruitingAiGenerateResponse> generateRecruitingAi(
            @RequestBody RecruitingAiGenerateRequest request
    ) {
        Long fixedUserId = 1L; // userId : 항상 1로 고정
        Type fixedType = Type.RECRUITING; // 항상 RECRUITING 고정

        RecruitingAiGenerateResponse response = recruitService.generateRecruitingAi(
                fixedUserId,
                fixedType,
                request.getPostImage(),
                request
        );

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 임시 저장
    @PostMapping("/draft")
    public ApiResponse<RecruitingDraftSaveResponse> saveRecruitingDraft(
            @RequestBody RecruitingDraftSaveRequest request
    ) {
        Long fixedUserId = 1L;
        Type fixedType = Type.RECRUITING;

        RecruitingDraftSaveResponse response =
                recruitService.saveOrUpdateDraft(fixedUserId, fixedType, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 제휴 홍보글 게시하기
    @PatchMapping("/{postId}/publish")
    public ApiResponse<RecruitingPublishResponse> publishRecruiting(@PathVariable Long postId) {

        RecruitingPublishResponse response = recruitService.publishRecruiting(postId);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ApiResponse<RecruitingDeleteResponse> deleteRecruiting(@PathVariable Long postId) {

        RecruitingDeleteResponse response = recruitService.deleteRecruiting(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }
}