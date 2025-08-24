package com.itzi.itzi.posts.dto.response;

import com.itzi.itzi.auth.domain.Category;
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
public class PostListResponse {

    private Long postId;
    private Long userId;

    private String category;
    private Type type;
    private Status status;

    private LocalDate exposureEndDate;
    private Long bookmarkCount;

    private String postImageUrl;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;

    private boolean targetNegotiable;
    private boolean periodNegotiable;
    private boolean benefitNegotiable;



}