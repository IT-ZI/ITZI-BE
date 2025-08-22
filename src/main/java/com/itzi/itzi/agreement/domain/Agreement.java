package com.itzi.itzi.agreement.domain;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.posts.domain.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id", nullable = false, unique = true)
    private Long agreementId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // 제휴 제안자
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    // 제휴 대상자
    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    // 제 1조(목적)
    @Column
    private String purpose;

    // 제 2조(대상 및 기간)
    @Column(name = "target_period")
    private String targetPeriod;

    // 제 3조(혜택 및 조건)
    @Column(name = "benefit_condition")
    private String benefitCondition;

    //제 4조(역할 및 의무)
    @Column
    private String role;

    // 제 5조(효력 및 해지)
    @Column
    private String effect;

    // 제 6조(기타)
    @Column
    private String etc;

    // 제휴업무 협약서(AI 변환 글 : 작성 전 null)
    @Column(columnDefinition = "TEXT")
    private String content;

    // 문서 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 엔티티가 db에 반영되기 직전에 자동으로 값이 채워짐! --> null 값 방지

    // 자동으로 시간 설정
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 자동으로 시간 업데이트
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @OneToOne(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Post post;

    public void setPost(Post post) {
        this.post = post;
        if (post.getAgreement() != this) {
            post.setAgreement(this);
        }
    }
}
