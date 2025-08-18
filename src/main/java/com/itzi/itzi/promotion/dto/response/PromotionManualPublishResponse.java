package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionManualPublishResponse {

    private Type type;
    private Status status;
    private Long postId;

    private String postImage;
    private String title;
    private String target;
    private LocalDate startDate;
    private LocalDate endDate;
    private String benefit;
    private String condition;
    private String content;
    private LocalDate exposureEndDate;

    private Boolean exposeProposerInfo;
    private Boolean exposeTargetInfo;

    private LocalDateTime publishedAt;
}
