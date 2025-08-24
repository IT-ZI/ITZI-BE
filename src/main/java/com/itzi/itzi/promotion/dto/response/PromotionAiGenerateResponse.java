package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionAiGenerateResponse {

    private Long postId;
    private Long userId;
    private Type type;
    private Status status;

    private String title;
    private String target;
    private String period;
    private String benefit;
    private String condition;
    private String content;

}
