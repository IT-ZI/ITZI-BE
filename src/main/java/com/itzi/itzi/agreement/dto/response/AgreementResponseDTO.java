package com.itzi.itzi.agreement.dto.response;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementResponseDTO {

    private Long agreementId;
    private String partnerDisplayName;
    private Status status;

    public static AgreementResponseDTO fromEntity(Agreement agreement, Long userId) {
        // partner 이름을 간단하게 잡아줌
        String partnerName = agreement.getSender().getUserId().equals(userId)
                ? agreement.getReceiverName()
                : agreement.getSenderName();

        return AgreementResponseDTO.builder()
                .agreementId(agreement.getAgreementId())
                .partnerDisplayName(partnerName)
                .status(agreement.getStatus())
                .build();
    }
}