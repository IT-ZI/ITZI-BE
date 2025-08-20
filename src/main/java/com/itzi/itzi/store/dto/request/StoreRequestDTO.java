package com.itzi.itzi.store.dto.request;

import com.itzi.itzi.auth.domain.Category;
import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreRequestDTO {
    // Store
    private String storeImage;
    private String name;
    private String info;
    private Category category;
    private String operatingHours;
    private String phone;       // store phone
    private String address;
    private String ownerName;
    private String linkUrl;
    private Integer rating;
    private Set<String> keywords;

    // User
    private String userProfileName;
    private String userName;
    private String userEmail;
    private String userPhone;           // user 기본 전화
    private String userProfileImage;
    private String userUniversity;
    private String userInterest;        // enum name (e.g. "BEAUTY")
}
