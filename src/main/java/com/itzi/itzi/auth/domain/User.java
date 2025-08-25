package com.itzi.itzi.auth.domain;

import com.itzi.itzi.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // 프로필 이름
    @Column(name = "profile_name", nullable = false, length = 10)
    private String profileName;

    // 사용자 본인 이름
    @Column(name = "user_name", nullable = false, length = 10)
    private String userName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "profile_image", nullable = false, length = 500)
    private String profileImage;

    @Column(nullable = false)
    private String university;

    @Enumerated(EnumType.STRING)
    @Column
    private Category interest;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 단건 조회를 위해 역방향 연관 필드 추가
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private OrgProfile orgProfile;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Store store;
}
