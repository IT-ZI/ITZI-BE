package com.itzi.itzi.promotion.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEditViewResponse {

    private Long postId;
    private String postImage;

    private String title;
    private String target;
    private String benefit;
    private String condition;
    private String content;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate exposureEndDate;

    private Boolean exposeProposerInfo;
    private Boolean exposeTargetInfo;

}
