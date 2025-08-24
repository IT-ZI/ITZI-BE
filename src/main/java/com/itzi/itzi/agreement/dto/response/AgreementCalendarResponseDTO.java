package com.itzi.itzi.agreement.dto.response;

import com.itzi.itzi.agreement.domain.Agreement;
import com.itzi.itzi.agreement.domain.Status;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementCalendarResponseDTO {

    private Long agreementId;
    private String title;      // "스타벅스 X 총학생회 제휴" 같은 캘린더 표시용
    private String partnerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;

    public static AgreementCalendarResponseDTO fromEntity(Agreement agreement, Long userId) {
        String partnerName = agreement.getSender().getUserId().equals(userId)
                ? agreement.getReceiverName()
                : agreement.getSenderName();

        String title = partnerName + " X " +
                (agreement.getSender().getUserId().equals(userId)
                        ? agreement.getSenderName()
                        : agreement.getReceiverName())
                + " 제휴";

        return AgreementCalendarResponseDTO.builder()
                .agreementId(agreement.getAgreementId())
                .partnerName(partnerName)
                .title(title)
                .startDate(agreement.getStartDate())   // ✅ DB에 저장된 값 그대로 사용
                .endDate(agreement.getEndDate())       // ✅ DB에 저장된 값 그대로 사용
                .status(agreement.getStatus())
                .build();
    }

}

