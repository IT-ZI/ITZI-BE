package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionPublishResponse {

    private Type type;
    private Status status;
    private Long postId;
    private LocalDateTime publishedAt;

}
