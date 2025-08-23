package com.itzi.itzi.posts.repository;

import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 내 글 + 타입 + 상태 필터
    List<Post> findByUser_UserIdAndTypeAndStatusIn(
            Long userId,
            Type type,
            Collection<Status> statuses
    );

    // postId와 type으로 게시글을 찾는 메서드 추가
    Optional<Post> findByPostIdAndType(Long postId, Type type);

    // 모든 사용자가 작성한 제휴 모집글 필터링 조회 : 인기순, 최신순, 오래된순
    List<Post> findByTypeAndStatus(Type type, Status status, Sort sort);

    // 모든 사용자가 작성한 제휴 모집글 필터링 조회 : 마감 임박순
    List<Post> findByTypeAndStatusAndExposureEndDateGreaterThanEqual(
            Type type, Status status, LocalDate today, Sort sort
    );


    /*
     1. Post 엔티티와 작성자(User)를 반드시 fetch join 으로 즉시 로딩
     2. 작성자(User)와 연결된 OrgProfile, Store 정보를 left join 으로 함께 패치
     */
    @Query("""
      select p 
      from Post p 
      join fetch p.user u 
      left join OrgProfile op on op.user = u
      left join Store s on s.user = u
      where p.postId = :postId and p.type = :type
    """)
    Optional<Post> findRecruitingDetailWithAuthor(@Param("postId") Long postId,
                                                  @Param("type") Type type);
}
