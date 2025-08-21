package com.itzi.itzi.partnership.dto.response;

import com.itzi.itzi.partnership.domain.Partnership;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipPostResponseDTO {
    private Long partnershipId;
    private Long senderId;
    private Long receiverId;
    private String purpose;
    private String periodType;
    private String periodValue;
    private String orgType;
    private String orgValue;
    private String detail;
    private String content;
    private String status;
    private Set<String> keywords;

    public static PartnershipPostResponseDTO fromEntity(Partnership p) {
        return PartnershipPostResponseDTO.builder()
                .partnershipId(p.getPartnershipId())
                .senderId(p.getSender() != null ? p.getSender().getUserId() : null)
                .receiverId(p.getReceiver() != null ? p.getReceiver().getUserId() : null)
                .purpose(p.getPurpose())
                .periodType(p.getPeriodType() != null ? p.getPeriodType().name() : null)
                .periodValue(p.getPeriodValue())
                .orgType(p.getOrgType() != null ? p.getOrgType().name() : null)
                .orgValue(p.getOrgValue())
                .detail(p.getDetail())
                .content(p.getContent())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .keywords(p.getKeywords())
                .build();
    }
}
