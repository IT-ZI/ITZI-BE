package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PromotionDraftSaveResponse {

    private Long postId;
    private Long userId;

    private Type type;                  // PROMOTION
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
