package com.itzi.itzi.promotion.dto.response;

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
    // 카테고리 추가 필요
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

    // 작성자 정보 블럭 추가 필요
}
