package com.itzi.itzi.store.dto.response;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.OrgType;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.store.domain.Store;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreResponseDTO {
    // User
    private Long userId;
    private String profileName;
    private String userName;
    private String email;
    private String phone;           // User 기본 연락처
    private String profileImage;
    private String university;
    private String interest;        // enum name (ex. BEAUTY)
    private String interestDescription; // 한글 설명 (ex. 뷰티/미용)
    private OrgType orgType;        // orgProfile 없고 store 있으면 STORE 로 세팅

    // Store
    private Long storeId;
    private String storeImage;
    private String storeName;
    private String info;
    private String address;
    private Category category; // ex) BEAUTY
    private String categoryDescription; // ex) 뷰티/미용
    private String operatingHours;
    private String storePhone;      // 매장 전화 (User Phone과 충돌 방지)
    private String ownerName;
    private String linkUrl;
    private Integer rating;
    private Set<String> keywords;

    public static StoreResponseDTO from(User user, Store store, OrgProfile orgProfile) {
        // orgType 결정: orgProfile 있으면 그대로, 없고 store 있으면 STORE
        OrgType resolvedOrgType = (orgProfile != null)
                ? orgProfile.getOrgType()
                : (store != null ? OrgType.STORE : null);

        Category storeCategory = (store != null ? store.getCategory() : null);

        return StoreResponseDTO.builder()
                // user
                .userId(user != null ? user.getUserId() : null)
                .profileName(user != null ? user.getProfileName() : null)
                .userName(user != null ? user.getUserName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .profileImage(user != null ? user.getProfileImage() : null)
                .university(user != null ? user.getUniversity() : null)
                .interest(user != null && user.getInterest() != null ? user.getInterest().name() : null)
                .interestDescription(user != null && user.getInterest() != null ? user.getInterest().getDescription() : null)
                .orgType(resolvedOrgType)

                // store
                .storeId(store != null ? store.getStoreId() : null)
                .storeImage(store != null ? store.getStoreImage() : null)
                .storeName(store != null ? store.getName() : null)
                .info(store != null ? store.getInfo() : null)
                .address(store != null ? store.getAddress() : null)
                .category(storeCategory)
                .categoryDescription(storeCategory != null ? storeCategory.getDescription() : null)
                .operatingHours(store != null ? store.getOperatingHours() : null)
                .storePhone(store != null ? store.getPhone() : null)
                .ownerName(store != null ? store.getOwnerName() : null)
                .linkUrl(store != null ? store.getLinkUrl() : null)
                .rating(store != null ? store.getRating() : null)
                .keywords(store != null ? store.getKeywords() : null)
                .build();
    }
}
