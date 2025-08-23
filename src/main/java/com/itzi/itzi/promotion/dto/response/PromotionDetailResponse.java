package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetailResponse {

    private Long postId;
    private Long userId;

    private LocalDate exposureEndDate;
    private Category category;
    private Long bookmarkCount;

    private Type type;
    private Status status;

    private String postImage;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;
    private String condition;
    private String content;

    private Object sender;              // 제휴 제안자
    private Object receiver;            // 제휴 대상자

}
