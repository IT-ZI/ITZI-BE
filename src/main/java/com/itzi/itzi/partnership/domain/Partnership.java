package com.itzi.itzi.partnership.domain;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private Status status;

    @ElementCollection
    @CollectionTable(name = "partnership_keywords", joinColumns = @JoinColumn(name = "partnership_id"))
    private Set<String> keywords = new HashSet<>();

    // 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 받는 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
}
