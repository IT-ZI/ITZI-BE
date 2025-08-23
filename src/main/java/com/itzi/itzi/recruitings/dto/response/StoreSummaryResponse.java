package com.itzi.itzi.recruitings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSummaryResponse {

    private Long userId;

    private String image;
    private Integer rating;
    private String name;
    private String info;
    private Set<String> keywords;

    private String category;
    private String operatingHours;
    private String phone;
    private String address;
    private String ownerName;
    private String linkUrl;

}
