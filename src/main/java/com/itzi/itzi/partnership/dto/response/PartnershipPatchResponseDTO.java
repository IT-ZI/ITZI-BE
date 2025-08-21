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
    private Long receiverId;
    private String content;
    private String status;

    public static PartnershipPatchResponseDTO fromEntity(Partnership p) {
        return PartnershipPatchResponseDTO.builder()
                .partnershipId(p.getPartnershipId())
                .senderId(p.getSender() != null ? p.getSender().getUserId() : null)
                .receiverId(p.getReceiver() != null ? p.getReceiver().getUserId() : null)
                .content(p.getContent())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .build();
    }
}
