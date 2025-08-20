package com.itzi.itzi.partnership.domain;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partnership_id", nullable = false, unique = true)
    private Long partnershipId;

    @Column(nullable = false)
    private String purpose;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 120)
    private PeriodType periodType = PeriodType.SAME_AS_POST;

    @Column(name = "period_value")
    private String periodValue;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false)
    private OrgType orgType = OrgType.AUTO;

    @Column(name = "org_value")
    private String orgValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable=false)
    private ActorType senderType;

    @Column(name = "sender_id", nullable=false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable=false)
    private ActorType receiverType;

    @Column(name = "receiver_id", nullable=false)
    private Long receiverId;

    @ManyToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "store_id", unique = true)
    private Store store;
}
