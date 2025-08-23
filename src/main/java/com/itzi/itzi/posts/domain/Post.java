package com.itzi.itzi.posts.domain;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.partnership.domain.Partnership;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type")
    private Type type;

    // 추후에 Store 엔티티 FK 예정

    private String postImage;

    @Column(length = 120)
    private String title;           // 모집글 제목

    @Column(length = 120)
    private String target;          // 제휴 대상

    // 제휴 기간
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length = 120)
    private String benefit;         // 제휴 혜택

    @Column(name = "`condition`", length = 120)
    private String condition;       // 제휴 조건

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate exposureEndDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean targetNegotiable = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean periodNegotiable = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean benefitNegotiable = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean conditionNegotiable = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;         // DRAFT, PUBLISHED, DELETED

    @Builder.Default
    private Long bookmarkCount = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    // 제휴 제안자, 대상자
    private Boolean exposeProposerInfo;
    private Boolean exposeTargetInfo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    // Post ↔ Partnership (1:N)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Partnership> partnerships = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

}