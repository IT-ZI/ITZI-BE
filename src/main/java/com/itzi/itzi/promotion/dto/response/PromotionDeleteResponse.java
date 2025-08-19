package com.itzi.itzi.promotion.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PromotionDeleteResponse {

    private Long postId;
    private Type type;
    private Status status;

}
