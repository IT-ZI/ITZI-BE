package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitGenerateAiResponse {

    private Long postId;
    private Long userId;
    private Type type;
    private Status status;

    private String image;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;
    private String condition;
    private String content;
    private LocalDate exposureEndDate;

}
