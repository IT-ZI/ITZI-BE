package com.itzi.itzi.posts.repository;

import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 내 글 + 타입 + 상태 필터
    List<Post> findByUserIdAndTypeAndStatusIn(
            Long userId,
            Type type,
            Collection<Status> statuses
    );

    // 모든 사용자가 작성한 제휴 모집글
    List<Post> findByTypeAndStatus(Type type, Status status);


}
