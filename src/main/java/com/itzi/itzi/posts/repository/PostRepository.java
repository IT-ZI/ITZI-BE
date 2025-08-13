package com.itzi.itzi.posts.repository;

import com.itzi.itzi.posts.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

}
