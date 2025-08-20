package com.itzi.itzi.auth.service;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.dto.request.UserOrgProfileRequestDTO;
import com.itzi.itzi.auth.dto.response.UserOrgProfileResponseDTO;
import com.itzi.itzi.auth.repository.OrgProfileRepository;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrgProfileRepository orgProfileRepository;

    @Transactional(readOnly = true)
    public UserOrgProfileResponseDTO getUserAndOrgProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        OrgProfile orgProfile = orgProfileRepository.findByUser(user)
                .orElse(null);

        return UserOrgProfileResponseDTO.from(user, orgProfile);
    }

    @Transactional
    public UserOrgProfileResponseDTO patchUserAndOrgProfile(Long userId, UserOrgProfileRequestDTO dto) {
        // 1) 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 유저를 찾을 수 없습니다. (userId=" + userId + ")"));

        OrgProfile orgProfile = orgProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("해당 유저의 사용자 유형 프로필을 찾을 수 없습니다. (userId=" + userId + ")"));

        // 2) User 부분 업데이트
        if (dto.profileName() != null)   user.setProfileName(dto.profileName());
        if (dto.userName() != null)      user.setUserName(dto.userName());
        if (dto.email() != null)         user.setEmail(dto.email());
        if (dto.phone() != null)         user.setPhone(dto.phone());
        if (dto.profileImage() != null)  user.setProfileImage(dto.profileImage());
        if (dto.university() != null)    user.setUniversity(dto.university());
        if (dto.interest() != null)      user.setInterest(parseCategory(dto.interest()));

        // 3) OrgProfile 부분 업데이트
        if (dto.orgType() != null)       orgProfile.setOrgType(dto.orgType());
        if (dto.schoolName() != null)    orgProfile.setSchoolName(dto.schoolName());
        if (dto.unitName() != null)      orgProfile.setUnitName(dto.unitName());
        if (dto.orgPhone() != null)      orgProfile.setPhone(dto.orgPhone());
        if (dto.address() != null)       orgProfile.setAddress(dto.address());
        if (dto.ownerName() != null)     orgProfile.setOwnerName(dto.ownerName());
        if (dto.linkUrl() != null)       orgProfile.setLinkUrl(dto.linkUrl());
        if (dto.intro() != null)         orgProfile.setIntro(dto.intro());
        if (dto.keywords() != null) {
            validateKeywords(dto.keywords());
            orgProfile.setKeywords(dto.keywords());
        }

        // 4) 필수 최종 상태 검증 (PATCH라도 최종 상태는 유효해야 함)
        ensureRequiredNonNull(user, orgProfile);

        // 5) 응답
        return UserOrgProfileResponseDTO.from(user, orgProfile);
    }

    // --- helpers ---

    private Category parseCategory(String raw) {
        try {
            return Category.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            // INVALID_TYPE로 래핑해 통일된 에러 응답
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "지원하지 않는 interest 값: " + raw);
        }
    }

    private void validateKeywords(Set<String> keywords) {
        if (keywords.size() > 5) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST, "keywords는 최대 5개까지 가능합니다.");
        }
        for (String k : keywords) {
            if (k != null && k.length() > 10) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST, "각 keyword는 최대 10자까지 가능합니다. (" + k + ")");
            }
        }
    }

    /*
     DB 최종 상태로서 반드시 존재해야 하는 필드 방어.
     기존 값 + DTO 반영 후 null이면 예외.
     */
    private void ensureRequiredNonNull(User user, OrgProfile org) {
        // User 필수 (entity에서 nullable=false인 것들 중심)
        if (isBlank(user.getProfileName()))  throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "profileName은 필수입니다.");
        if (isBlank(user.getUserName()))     throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "userName은 필수입니다.");
        if (isBlank(user.getEmail()))        throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "email은 필수입니다.");
        if (isBlank(user.getPhone()))        throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "phone은 필수입니다.");
        if (isBlank(user.getProfileImage())) throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "profileImage은 필수입니다.");
        if (isBlank(user.getUniversity()))   throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "university는 필수입니다.");

        // OrgProfile 필수
        if (org.getOrgType() == null)        throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "orgType은 필수입니다.");
        if (isBlank(org.getSchoolName()))    throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "schoolName은 필수입니다.");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
