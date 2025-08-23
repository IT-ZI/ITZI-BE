package com.itzi.itzi.agreement.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementRequestDTO {

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

    // content 는 초기에 null 일 수 있음
    private String content;
}
