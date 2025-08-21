package com.itzi.itzi.partnership.dto.response;

import com.itzi.itzi.partnership.domain.Partnership;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipPatchResponseDTO {
    private Long partnershipId;

    private Long senderId;
    private String senderDisplayName;
    private Long receiverId;
    private String receiverDisplayName;

    private String content;
    private String sendStatus;
    private String acceptedStatus;

    public static PartnershipPatchResponseDTO fromEntity(Partnership p) {
        return PartnershipPatchResponseDTO.builder()
                .partnershipId(p.getPartnershipId())
                .senderId(p.getSender() != null ? p.getSender().getUserId() : null)
                .senderDisplayName(PartnershipPostResponseDTO.resolveDisplayName(p.getSender()))
                .receiverId(p.getReceiver() != null ? p.getReceiver().getUserId() : null)
                .receiverDisplayName(PartnershipPostResponseDTO.resolveDisplayName(p.getReceiver()))
                .content(p.getContent())
                .sendStatus(p.getSendStatus() != null ? p.getSendStatus().name() : null)
                .acceptedStatus(p.getAcceptedStatus() != null ? p.getAcceptedStatus().name() : null)
                .build();
    }
}
