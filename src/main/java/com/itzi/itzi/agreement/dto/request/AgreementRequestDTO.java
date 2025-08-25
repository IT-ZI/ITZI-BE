package com.itzi.itzi.agreement.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementRequestDTO {

    private Long partnershipId; // 필수
    private Long postId;        // 프론트에서 같이 넘겨줌

    private String purpose;
    private String targetPeriod;
    private String benefitCondition;
    private String role;
    private String effect;
    private String etc;
    private String content; // content는 초기 null 가능
}

