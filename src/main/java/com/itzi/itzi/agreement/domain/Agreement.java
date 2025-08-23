package com.itzi.itzi.agreement.domain;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.partnership.domain.Partnership;
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

    // Partnership과 1:1 (필수)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;

    // Post와 N:1 (필수)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    private String purpose;

    @Column(name = "target_period")
    private String targetPeriod;

    @Column(name = "benefit_condition")
    private String benefitCondition;

    private String role;
    private String effect;
    private String etc;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 편의 메서드
    public void setPartnership(Partnership partnership) {
        this.partnership = partnership;
        if (partnership.getAgreement() != this) {
            partnership.setAgreement(this);
        }
    }

    // =============================
    // 상태 전환 메서드
    // =============================
    public void generate() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 문서 변환 가능");
        }
        this.status = Status.GENERATED;
    }

    public void signAsSender(User sender) {
        if (!this.sender.equals(sender)) {
            throw new IllegalArgumentException("송신자만 서명할 수 있음");
        }
        if (this.status != Status.GENERATED) {
            throw new IllegalStateException("GENERATED 상태에서만 송신자 서명 가능");
        }
        this.status = Status.SIGNED_SENDER;
    }

    public void sendToReceiver() {
        if (this.status != Status.SIGNED_SENDER) {
            throw new IllegalStateException("송신자 서명 완료 상태에서만 전송 가능");
        }
        this.status = Status.SENT;
    }

    public void signAsReceiver(User receiver) {
        if (!this.receiver.equals(receiver)) {
            throw new IllegalArgumentException("수신자만 서명할 수 있음");
        }
        if (this.status != Status.SENT) {
            throw new IllegalStateException("SENT 상태에서만 수신자 서명 가능");
        }
        this.status = Status.SIGNED_RECEIVER;
    }

    public void markAllSigned() {
        if (this.status != Status.SIGNED_RECEIVER) {
            throw new IllegalStateException("수신자 서명 후에만 양측 서명 완료 처리 가능");
        }
        this.status = Status.SIGNED_ALL;
    }

    public void approve() {
        if (this.status != Status.SIGNED_ALL) {
            throw new IllegalStateException("양측 서명 완료 후에만 승인 가능");
        }
        this.status = Status.APPROVED;
    }
}
