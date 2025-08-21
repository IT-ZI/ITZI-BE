package com.itzi.itzi.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrgProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id", nullable = false, unique = true)
    private Long orgId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgType orgType;

    // 학교/동아리/단체 명
    @Column(name = "school_name", nullable = false, length = 120)
    private String schoolName;

    @Column(name = "unit_name", length = 120)
    private String unitName;

    @Column(length = 20)
    private String phone;

    @Column
    private String address;

    @Column(name = "owner_name", length = 60)
    private String ownerName;

    @Column(name = "link_url")
    private String linkUrl;

    // 한 줄 소개
    @Column(length = 100)
    private String intro;

    // 별점
    @Column
    private Integer rating;

    // 키워드 (최대 5개까지 사용자가 입력)
    /* @ElementCollection : Enum 값을 별도 테이블에 저장
       @CollectionTable : 부모 테이블의 PK를 FK로 가지는 연결 테이블 생성 */
    @ElementCollection
    @CollectionTable(
            name = "org_profile_keywords",
            joinColumns = @JoinColumn(name = "org_id")
    )
    @Column(name = "keyword", length = 10, nullable = false)
    // 키워드 중복 X, 순서 없이 저장
    @Builder.Default
    private Set<String> keywords = new HashSet<>();

}