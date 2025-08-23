package com.itzi.itzi.recruitings.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RecruitingPublishResponse {

    private Type type;
    private Long postId;
    private Status status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "Asia/Seoul")
    private LocalDateTime publishedAt;

}