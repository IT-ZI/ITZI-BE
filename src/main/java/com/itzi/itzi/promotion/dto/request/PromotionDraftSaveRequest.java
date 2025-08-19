package com.itzi.itzi.promotion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class PromotionDraftSaveRequest {

    // 존재할 경우 해당 DRAFT 글 업데이트, 없으면 생성
    private Long postId;

    private MultipartFile postImage;
    private String title;
    private String target;

    private LocalDate startDate;
    private LocalDate endDate;

    private String benefit;
    private String condition;
    private String content;

    private LocalDate exposureEndDate;

    private Boolean exposeProposerInfo;
    private Boolean exposeTargetInfo;


}
