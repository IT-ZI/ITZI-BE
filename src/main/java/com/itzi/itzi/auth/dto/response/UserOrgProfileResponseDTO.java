package com.itzi.itzi.auth.dto.response;

import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.domain.OrgType;

import java.util.Set;

public record UserOrgProfileResponseDTO(
        // User
        Long userId,
        String profileName,
        String userName,
        String email,
        String phone,
        String profileImage,
        String university,
        String interest,

        // OrgProfile
        Long orgId,
        OrgType orgType,
        String schoolName,
        String unitName,
        String orgPhone,
        String address,
        String ownerName,
        String linkUrl,
        String intro,
        Set<String> keywords
) {
    public static UserOrgProfileResponseDTO from(User user, OrgProfile orgProfile) {
        return new UserOrgProfileResponseDTO(
                user.getUserId(),
                user.getProfileName(),
                user.getUserName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImage(),
                user.getUniversity(),
                user.getInterest() != null ? user.getInterest().name() : null,

                orgProfile != null ? orgProfile.getOrgId() : null,
                orgProfile != null ? orgProfile.getOrgType() : null,
                orgProfile != null ? orgProfile.getSchoolName() : null,
                orgProfile != null ? orgProfile.getUnitName() : null,
                orgProfile != null ? orgProfile.getPhone() : null,
                orgProfile != null ? orgProfile.getAddress() : null,
                orgProfile != null ? orgProfile.getOwnerName() : null,
                orgProfile != null ? orgProfile.getLinkUrl() : null,
                orgProfile != null ? orgProfile.getIntro() : null,
                orgProfile != null ? orgProfile.getKeywords() : null
        );
    }
}
