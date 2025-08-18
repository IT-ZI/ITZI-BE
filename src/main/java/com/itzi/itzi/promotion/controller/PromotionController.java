package com.itzi.itzi.promotion.controller;

import com.itzi.itzi.global.api.code.SuccessStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.promotion.dto.request.PromotionManualPublishRequest;
import com.itzi.itzi.promotion.dto.response.PromotionManualPublishResponse;
import com.itzi.itzi.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
