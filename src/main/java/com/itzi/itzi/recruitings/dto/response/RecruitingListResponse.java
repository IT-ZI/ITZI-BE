package com.itzi.itzi.recruitings.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitingListResponse {

    private Type type;

    private Long postId;
    private Long userId;

    private MultipartFile postImageUrl;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;

    private boolean targetNegotiable;
    private boolean periodNegotiable;
    private boolean benefitNegotiable;

    private Long bookmarkCount;

    private Status status;

}