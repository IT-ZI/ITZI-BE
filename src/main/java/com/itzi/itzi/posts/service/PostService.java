package com.itzi.itzi.posts.service;

import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.dto.response.PostDeleteResponse;
import com.itzi.itzi.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    // 제휴 홍보글 삭제하기
    @Transactional
    public PostDeleteResponse deletePost(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 게시된 홍보글만 삭제 가능
        if (post.getStatus() == Status.DELETED || post.getStatus() == Status.DRAFT) {
            throw new GeneralException(ErrorStatus.CANNOT_DELETE_POST);
        }

        post.setStatus(Status.DELETED);
        postRepository.save(post);

        return new PostDeleteResponse(
                post.getType(),
                post.getPostId(),
                post.getStatus()
        );
    }
}
