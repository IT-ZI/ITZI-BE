package com.itzi.itzi.partnership.dto.request;

import com.itzi.itzi.partnership.domain.OrgType;
import com.itzi.itzi.partnership.domain.PeriodType;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipPostRequestDTO {
    private Long receiverId;
    private String purpose;
    private PeriodType periodType;
    private String periodValue;
    private OrgType orgType;
    private String orgValue;
    private String detail;
    private Set<String> keywords;
}
