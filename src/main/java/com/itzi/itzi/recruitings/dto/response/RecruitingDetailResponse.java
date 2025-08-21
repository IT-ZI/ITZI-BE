package com.itzi.itzi.recruitings.dto.response;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitingDetailResponse {

    private Long postId;
    private Long userId;

    private LocalDate exposureEndDate;
    private Long bookmarkCount;

    // 카테고리
    private Category category;          // FOOD, FASHION, BEAUTY, HEALTH, BOOK, LIVING, HOSPITAL, IT, TRANSPORTATION, ETC

    private Type type;                  // RECRUITING, PROMOTION
    private Status status;              // DRAFT, PUBLISHED

    private String title;
    private String postImageUrl;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;
    private String condition;

    private boolean targetNegotiable;
    private boolean periodNegotiable;
    private boolean benefitNegotiable;
    private boolean conditionNegotiable;

    private String content;

    // 작성자 정보 블럭
    private Object author;

}
