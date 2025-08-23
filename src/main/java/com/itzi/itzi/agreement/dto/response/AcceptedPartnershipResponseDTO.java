package com.itzi.itzi.agreement.dto.response;

import com.itzi.itzi.partnership.domain.Partnership;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptedPartnershipResponseDTO {

    private Long partnershipId;
    private String partnerDisplayName;

    public static AcceptedPartnershipResponseDTO fromEntity(
            Partnership partnership,
            String partnerDisplayName
    ) {
        return AcceptedPartnershipResponseDTO.builder()
                .partnershipId(partnership.getPartnershipId())
                .partnerDisplayName(partnerDisplayName)
                .build();
    }
}
