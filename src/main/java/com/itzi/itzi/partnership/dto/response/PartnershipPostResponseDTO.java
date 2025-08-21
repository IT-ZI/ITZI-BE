package com.itzi.itzi.partnership.dto.response;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.partnership.domain.Partnership;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipPostResponseDTO {

    private Long partnershipId;
    private Long senderId;
    private String senderDisplayName;  // 문의함에서 이름만 띄울 때 사용
    private Long receiverId;
    private String receiverDisplayName; // 문의함에서 이름만 띄울 때 사용

    private String purpose;
    private String periodType;
    private String periodValue;
    private String orgType;
    private String orgValue;
    private String detail;
    private String content;
    private java.util.Set<String> keywords;

    private String sendStatus;
    private String acceptedStatus;

    public static PartnershipPostResponseDTO fromEntity(Partnership p) {
        return PartnershipPostResponseDTO.builder()
                .partnershipId(p.getPartnershipId())
                .senderId(p.getSender() != null ? p.getSender().getUserId() : null)
                .senderDisplayName(resolveDisplayName(p.getSender()))      // 👈 사용
                .receiverId(p.getReceiver() != null ? p.getReceiver().getUserId() : null)
                .receiverDisplayName(resolveDisplayName(p.getReceiver()))  // 👈 사용
                .purpose(p.getPurpose())
                .periodType(p.getPeriodType() != null ? p.getPeriodType().name() : null)
                .periodValue(p.getPeriodValue())
                .orgType(p.getOrgType() != null ? p.getOrgType().name() : null)
                .orgValue(p.getOrgValue())
                .detail(p.getDetail())
                .content(p.getContent())
                .keywords(p.getKeywords())
                .sendStatus(p.getSendStatus() != null ? p.getSendStatus().name() : null)
                .acceptedStatus(p.getAcceptedStatus() != null ? p.getAcceptedStatus().name() : null)
                .build();
    }

    // 👇 외부에서 호출할 수 있게 public static
    public static String resolveDisplayName(User u) {
        if (u == null) return null;

        // 1) 점포가 있으면 점포명 우선
        if (u.getStore() != null && u.getStore().getName() != null && !u.getStore().getName().isBlank()) {
            return u.getStore().getName();
        }
        // 2) 조직 프로필이 있으면 학교/단과/동아리 조합
        if (u.getOrgProfile() != null) {
            String school = u.getOrgProfile().getSchoolName();
            String unit   = u.getOrgProfile().getUnitName();
            if (school != null && unit != null && !unit.isBlank()) return school + " " + unit;
            if (school != null && !school.isBlank()) return school;
        }
        // 3) 없으면 프로필명/유저명
        if (u.getProfileName() != null && !u.getProfileName().isBlank()) return u.getProfileName();
        return u.getUserName();
    }
}
