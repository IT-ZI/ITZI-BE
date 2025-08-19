package com.itzi.itzi.promotion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class PromotionManualPublishRequest {


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
