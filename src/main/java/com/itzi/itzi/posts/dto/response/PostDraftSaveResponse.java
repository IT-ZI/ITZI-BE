package com.itzi.itzi.posts.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PostDraftSaveResponse {

    private Type type;
    private Long postId;
    private Long userId;
    private Status status;
    private LocalDateTime updatedAt;
}