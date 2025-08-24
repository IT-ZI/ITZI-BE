package com.itzi.itzi.recruitings.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.OrderBy;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.dto.response.*;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.posts.dto.request.PostDraftSaveRequest;
import com.itzi.itzi.recruitings.dto.response.*;
import com.itzi.itzi.recruitings.service.RecruitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ApiResponse<PostDraftSaveResponse> saveRecruitingDraft(
            @ModelAttribute PostDraftSaveRequest request
    ) {
        Long fixedUserId = 1L;
        Type fixedType = Type.RECRUITING;


        PostDraftSaveResponse response =
                recruitService.saveOrUpdateDraft(fixedUserId, fixedType, request);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 제휴 홍보글 게시하기
    @PatchMapping("/{postId}/publish")
    public ApiResponse<PostPublishResponse> publishRecruiting(@PathVariable Long postId) {

        PostPublishResponse response = recruitService.publishRecruiting(postId);

        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ApiResponse<PostDeleteResponse> deleteRecruiting(@PathVariable Long postId) {

        PostDeleteResponse response = recruitService.deleteRecruiting(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 작성한 게시글 단건 상세 내용 조회
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getRecruitingDetail(@PathVariable Long postId) {

        PostDetailResponse response = recruitService.getRecruitingDetail(postId);
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 내가 작성한 게시글 전체 조회 (userId = 1)
    @GetMapping("/mine")
    public ApiResponse<List<PostListResponse>> getMyRecruitingList() {
        List<PostListResponse> response = recruitService.getMyRecruitingList();
        return ApiResponse.of(SuccessStatus._OK, response);
    }

    // 모든 사용자가 작성한 제휴 모집글 조회
    @GetMapping("/all")
    public ApiResponse<List<PostListResponse>> getAllRecruitingList(
            @RequestParam(defaultValue = "CLOSING") OrderBy orderBy,
            @RequestParam(required = false) List<String> filters
    ) {
        List<PostListResponse> responses = recruitService.getAllRecruitingList(orderBy, filters);
        return ApiResponse.of(SuccessStatus._OK, responses);
    }

    @GetMapping()
    public ApiResponse<Page<PostListResponse>> getPosts(
            @RequestParam(required = false, defaultValue = "전체") String orgType,
            @PageableDefault(size = 12, page = 0) Pageable pageable
    ) {
        Page<PostListResponse> posts = recruitService.getPostsByOrgType(orgType, pageable);
        return ApiResponse.of(SuccessStatus._OK, posts);
    }
}