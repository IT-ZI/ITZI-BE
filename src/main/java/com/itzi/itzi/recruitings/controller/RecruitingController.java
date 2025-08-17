package com.itzi.itzi.recruitings.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.recruitings.dto.request.RecruitingDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.*;
import com.itzi.itzi.recruitings.service.RecruitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recruiting")
@RequiredArgsConstructor
@Slf4j
public class RecruitingController {

    private final RecruitService recruitService;

    // 작성된 기본 정보를 바탕으로 상세 내용 생성
    @PostMapping(value = "/ai")
    public ApiResponse<RecruitingAiGenerateResponse> generateRecruitingAi(
            @ModelAttribute RecruitingAiGenerateRequest request   // 텍스트 + 파일 동시 바인딩
    ) {
        Long fixedUserId = 1L;                 // 항상 1
        Type fixedType = Type.RECRUITING;      // 항상 RECRUITING

        RecruitingAiGenerateResponse response =
                recruitService.generateRecruitingAi(fixedUserId, fixedType, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 임시 저장
    @PostMapping("/draft")
    public ApiResponse<RecruitingDraftSaveResponse> saveRecruitingDraft(
            @ModelAttribute RecruitingDraftSaveRequest request
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

    // 작성한 게시글 단건 상세 내용 조회
    @GetMapping("/{postId}")
    public ApiResponse<RecruitingDetailResponse> getRecruitingDetail(@PathVariable Long postId) {

        RecruitingDetailResponse response = recruitService.getRecruitingDetail(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 내가 작성한 게시글 전체 조회 (userId = 1)
    @GetMapping("/mine")
    public ApiResponse<List<RecruitingListResponse>> getMyRecruitingList(
            @RequestParam(defaultValue = "RECRUITING") Type type
    ) {
        List<RecruitingListResponse> response = recruitService.getMyRecruitingList(type);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 모든 사용자가 작성한 제휴 모집글 조회
    @GetMapping("/all")
    public ApiResponse<List<RecruitingListResponse>> getAllRecruitingList(
            @RequestParam(defaultValue = "RECRUITING") Type type
    ) {
        List<RecruitingListResponse> responses = recruitService.getAllRecruitingList(type);
        return ApiResponse.of(SuccessStatus._OK, responses);
    }
}