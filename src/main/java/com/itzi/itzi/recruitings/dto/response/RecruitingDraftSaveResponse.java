package com.itzi.itzi.recruitings.dto.response;

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
public class RecruitingDraftSaveResponse {

    private Type type;              // RECRUITING으로 고정
    private Long postId;
    private Long userId;
    private Status status;
    private LocalDateTime updatedAt;
}