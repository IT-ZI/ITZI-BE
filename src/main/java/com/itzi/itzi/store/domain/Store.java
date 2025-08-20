package com.itzi.itzi.store.domain;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.User;
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
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id", nullable = false, unique = true)
    private Long storeId;

    @Column(name = "store_image", nullable = false)
    private String storeImage;

    // 매장 이름
    @Column(nullable = false)
    private String name;

    // 매장 소개
    @Column(nullable = false)
    private String info;

    // 업종 (user 패키지 안에 존재)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "operating_hours", nullable = false)
    private String operatingHours;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "link_url", nullable = false)
    private String linkUrl;

    @Column(nullable = false)
    private Integer rating;

    // 키워드 (최대 5개까지 사용자가 입력)
    /* @ElementCollection : Enum 값을 별도 테이블에 저장
       @CollectionTable : 부모 테이블의 PK를 FK로 가지는 연결 테이블 생성 */
    @ElementCollection
    @CollectionTable(
            name = "store_keywords",
            joinColumns = @JoinColumn(name = "store_id")
    )
    @Column(name = "keyword", length = 10, nullable = false) // DB는 여유 있게 50자로 둠
    // 키워드 중복 X, 순서 X
    private Set<String> keywords = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)        // 사용자당 점포 1개
    private User user;
}
