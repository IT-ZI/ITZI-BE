package com.itzi.itzi.posts.service;

import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.dto.response.PostDeleteResponse;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.posts.dto.response.PostPublishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    // 제휴 홍보글 게시하기
    @Transactional
    public PostPublishResponse pusblishPost(Long postId) {

        // 존재하는 게시글인지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 이미 게시된 글인지 확인
        if (post.getStatus() == Status.PUBLISHED) {
            throw new GeneralException(ErrorStatus.ALREADY_PUBLISHED);
        }

        // 제휴 모집글 게시를 위해서는 모든 필드가 작성돼야 함
        if (post.getPostImage() == null ||
                post.getTitle() == null || post.getTitle().isBlank() ||
                post.getTarget() == null || post.getTarget().isBlank() ||
                post.getStartDate() == null || post.getEndDate() == null ||
                post.getBenefit() ==  null || post.getBenefit().isBlank() ||
                post.getCondition() == null || post.getCondition().isBlank() ||
                post.getContent() == null || post.getContent().isBlank() ||
                post.getExposureEndDate() == null ) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }

        // 게시 상태로 변경 및 생성 시간 업데이트
        post.setStatus(Status.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());

        postRepository.save(post);

        return new PostPublishResponse(
                post.getType(),
                post.getPostId(),
                post.getStatus(),
                post.getPublishedAt()
        );
    }
}
