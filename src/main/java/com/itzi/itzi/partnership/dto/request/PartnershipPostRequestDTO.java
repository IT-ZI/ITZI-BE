package com.itzi.itzi.partnership.dto.request;

import com.itzi.itzi.partnership.domain.OrgType;
import com.itzi.itzi.partnership.domain.PeriodType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnershipPostRequestDTO {
    private Long receiverId;
    private Long postId;   // ✅ 추가 (어떤 모집글에 대한 문의인지 반드시 필요)

    private String purpose;
    private PeriodType periodType;
    private String periodValue;
    private OrgType orgType;
    private String orgValue;
    private String detail;

    // 리스트 크기 제한: 최대 5개
    @Size(max = 5, message = "키워드는 최대 5개까지만 입력 가능합니다.")
    // 각 문자열 길이 제한: 10자 이내
    private List<@Size(max = 10, message = "각 키워드는 10자 이내여야 합니다.") String> keywords;
}