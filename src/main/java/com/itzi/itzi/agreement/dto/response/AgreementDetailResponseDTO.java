package com.itzi.itzi.agreement.dto.response;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementDetailResponseDTO {

    private Long agreementId;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String receiverName;

    private String purpose;
    private String targetPeriod;
    private String benefitCondition;
    private String role;
    private String effect;
    private String etc;
    private String content;

    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 제휴 협약서 작성 중 제휴 모집글, 문의글 조회할 때 사용
    private Long partnershipId;
    private Long postId;

    // 협약서에서 파싱된 데이터 저장용
    private LocalDate startDate;
    private LocalDate endDate;

    public static AgreementDetailResponseDTO fromEntity(Agreement agreement) {
        return AgreementDetailResponseDTO.builder()
                .agreementId(agreement.getAgreementId())
                .senderId(agreement.getSender().getUserId())
                .receiverId(agreement.getReceiver().getUserId())
                .senderName(agreement.getSenderName())
                .receiverName(agreement.getReceiverName())
                .purpose(agreement.getPurpose())
                .targetPeriod(agreement.getTargetPeriod())
                .startDate(agreement.getStartDate())
                .endDate(agreement.getEndDate())
                .benefitCondition(agreement.getBenefitCondition())
                .role(agreement.getRole())
                .effect(agreement.getEffect())
                .etc(agreement.getEtc())
                .content(agreement.getContent())
                .status(agreement.getStatus())
                .createdAt(agreement.getCreatedAt())
                .updatedAt(agreement.getUpdatedAt())
                .partnershipId(
                        agreement.getPartnership() != null ? agreement.getPartnership().getPartnershipId() : null
                )
                .postId(
                        agreement.getPost() != null ? agreement.getPost().getPostId() : null
                )
                .build();
    }

}
