package com.itzi.itzi.recruitings.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitingAiGenerateResponse {

    private Long postId;
    private Long userId;

    private Type type;              // RECRUITING으로 고정
    private String postImage;       // Url로 반환
    private String title;

    private String target;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String benefit;
    private String condition;

    private String content;         // AI가 생성해 준 상세 내용

    private boolean targetNegotiable;
    private boolean periodNegotiable;
    private boolean benefitNegotiable;
    private boolean conditionNegotiable;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate exposureEndDate;

    private Status status;
    private Long bookmarkCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

}