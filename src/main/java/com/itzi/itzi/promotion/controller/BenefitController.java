package com.itzi.itzi.promotion.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.promotion.dto.request.BenefitGenerateAiRequest;
import com.itzi.itzi.promotion.dto.response.BenefitGenerateAiResponse;
import com.itzi.itzi.promotion.service.BenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
