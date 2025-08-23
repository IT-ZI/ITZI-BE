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

    // 제 4조(역할 및 의무)
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

    // 엔티티가 DB에 반영되기 직전에 자동으로 값이 채워짐
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 엔티티 업데이트 시 자동으로 시간 업데이트
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

    // =============================
    // 상태 전환 메서드
    // =============================

    // Draft → Generated
    public void generate() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 문서 변환 가능");
        }
        this.status = Status.GENERATED;
    }

    // Generated → Signed_Sender
    public void signAsSender(User sender) {
        if (!this.sender.equals(sender)) {
            throw new IllegalArgumentException("송신자만 서명할 수 있음");
        }
        if (this.status != Status.GENERATED) {
            throw new IllegalStateException("GENERATED 상태에서만 송신자 서명 가능");
        }
        this.status = Status.SIGNED_SENDER;
    }

    // Signed_Sender → Sent
    public void sendToReceiver() {
        if (this.status != Status.SIGNED_SENDER) {
            throw new IllegalStateException("송신자 서명 완료 상태에서만 전송 가능");
        }
        this.status = Status.SENT;
    }

    // Sent → Signed_Receiver
    public void signAsReceiver(User receiver) {
        if (!this.receiver.equals(receiver)) {
            throw new IllegalArgumentException("수신자만 서명할 수 있음");
        }
        if (this.status != Status.SENT) {
            throw new IllegalStateException("SENT 상태에서만 수신자 서명 가능");
        }
        this.status = Status.SIGNED_RECEIVER;
    }

    // Signed_Receiver → Signed_All
    public void markAllSigned() {
        if (this.status != Status.SIGNED_RECEIVER) {
            throw new IllegalStateException("수신자 서명 후에만 양측 서명 완료 처리 가능");
        }
        this.status = Status.SIGNED_ALL;
    }

    // Signed_All → Approved
    public void approve() {
        if (this.status != Status.SIGNED_ALL) {
            throw new IllegalStateException("양측 서명 완료 후에만 승인 가능");
        }
        this.status = Status.APPROVED;
    }
}
