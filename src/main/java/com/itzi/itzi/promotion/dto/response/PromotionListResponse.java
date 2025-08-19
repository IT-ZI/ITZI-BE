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
public class PromotionListResponse {

    private Long postId;
    private Long userId;
    private Type type;
    private Status status;

    private Long bookmarkCount;
    private LocalDate exposureEndDate;

    private String postImage;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;

}
