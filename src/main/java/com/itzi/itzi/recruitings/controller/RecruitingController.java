package com.itzi.itzi.recruitings.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.recruitings.dto.request.RecruitingAiGenerateRequest;
import com.itzi.itzi.recruitings.dto.response.RecruitingAiGenerateResponse;
import com.itzi.itzi.recruitings.service.RecruitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recruiting")
@RequiredArgsConstructor
@Slf4j
public class RecruitingController {

    private final RecruitService recruitService;

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
}
