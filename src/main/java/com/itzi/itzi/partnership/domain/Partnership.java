package com.itzi.itzi.partnership.domain;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.agreement.domain.Agreement;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Partnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partnership_id", nullable = false, unique = true)
    private Long partnershipId;

    private String purpose;

    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    private String periodValue;

    @Enumerated(EnumType.STRING)
    private OrgType orgType;

    private String orgValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false)
    private SendStatus sendStatus = SendStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "accepted_status", nullable = false)
    private AcceptedStatus acceptedStatus = AcceptedStatus.WAITING;

    @ElementCollection
    @CollectionTable(name = "partnership_keywords", joinColumns = @JoinColumn(name = "partnership_id"))
    private Set<String> keywords = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToOne(mappedBy = "partnership", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Agreement agreement;

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
        if (agreement.getPartnership() != this) {
            agreement.setPartnership(this);
        }
    }

    // ✅ 커스텀 빌더 생성자
    @Builder
    public Partnership(User sender, User receiver, Post post,
                       String purpose, PeriodType periodType, String periodValue,
                       OrgType orgType, String orgValue, String detail, String content,
                       SendStatus sendStatus, AcceptedStatus acceptedStatus, Set<String> keywords) {
        this.sender = sender;
        this.receiver = receiver;
        this.post = post; // 반드시 세팅
        this.purpose = purpose;
        this.periodType = periodType;
        this.periodValue = periodValue;
        this.orgType = orgType;
        this.orgValue = orgValue;
        this.detail = detail;
        this.content = content;
        this.sendStatus = sendStatus != null ? sendStatus : SendStatus.DRAFT;
        this.acceptedStatus = acceptedStatus != null ? acceptedStatus : AcceptedStatus.WAITING;
        this.keywords = keywords != null ? keywords : new HashSet<>();
    }
}
