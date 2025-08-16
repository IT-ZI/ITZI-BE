package com.itzi.itzi.recruitings.dto.response;

import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RecruitingDeleteResponse {

    private Type type;
    private Long postId;
    private Status status;

}
