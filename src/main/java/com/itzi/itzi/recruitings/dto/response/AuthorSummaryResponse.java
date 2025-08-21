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
public class AuthorSummaryResponse {

    private String image;
    private Integer rating;
    private String name;
    private String info;
    private Set<String> keywords;

    private String schoolName;
    private String unitName;
    private String phone;
    private String address;
    private String ownerName;
    private String linkUrl;

}