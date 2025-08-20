package com.itzi.itzi.store.service;

import com.itzi.itzi.auth.domain.Category;
import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.OrgProfileRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.exception.NotFoundException;
import com.itzi.itzi.store.domain.Store;
import com.itzi.itzi.store.dto.request.StoreRequestDTO;
import com.itzi.itzi.store.dto.response.StoreResponseDTO;
import com.itzi.itzi.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final OrgProfileRepository orgProfileRepository;

    // =========================
    // [userId 기반] 단건 조회
    // =========================
    @Transactional(readOnly = true)
    public StoreResponseDTO getStoreByUserId(Long userId) {
        Store store = storeRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("해당 user의 store가 없습니다. userId=" + userId));

        User user = store.getUser(); // FK(1:1)
        OrgProfile orgProfile = (user != null)
                ? orgProfileRepository.findByUser(user).orElse(null)
                : null;

        return StoreResponseDTO.from(user, store, orgProfile);
    }

    // =========================
    // [userId 기반] 부분 수정 (Store + User 동시 패치)
    // =========================
    @Transactional
    public StoreResponseDTO patchStoreByUserId(Long userId, StoreRequestDTO dto) {
        Store store = storeRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("해당 user의 store가 없습니다. userId=" + userId));

        // 1) Store 필드 패치
        applyStorePatch(store, dto);

        // 2) User 필드 패치 (옵셔널)
        User user = store.getUser();
        if (user == null) {
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR, "Store에 연결된 User가 없습니다.");
        }
        applyUserPatch(user, dto);

        // 3) 필수값 검증
        ensureRequiredNonNull(store);

        // 4) 저장 (User는 영속 상태이므로 더티체킹)
        Store saved = storeRepository.save(store);

        OrgProfile orgProfile = orgProfileRepository.findByUser(user).orElse(null);
        return StoreResponseDTO.from(user, saved, orgProfile);
    }

    // ==================================================
    // (구버전) storeId 기반 메서드 — 사용하지 않음 (참고용)
    // ==================================================
    /*
    @Transactional(readOnly = true)
    public StoreResponseDTO getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("해당 store가 존재하지 않습니다. id=" + storeId));
        User user = store.getUser();
        OrgProfile orgProfile = (user != null) ? orgProfileRepository.findByUser(user).orElse(null) : null;
        return StoreResponseDTO.from(user, store, orgProfile);
    }

    @Transactional
    public StoreResponseDTO patchStore(Long storeId, StoreRequestDTO dto) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("해당 store가 존재하지 않습니다. id=" + storeId));
        applyStorePatch(store, dto);
        ensureRequiredNonNull(store);
        Store saved = storeRepository.save(store);
        User user = saved.getUser();
        OrgProfile orgProfile = (user != null) ? orgProfileRepository.findByUser(user).orElse(null) : null;
        return StoreResponseDTO.from(user, saved, orgProfile);
    }
    */

    // ====== Store PATCH 적용 ======
    private void applyStorePatch(Store store, StoreRequestDTO dto) {
        if (dto.getStoreImage() != null) store.setStoreImage(dto.getStoreImage());
        if (dto.getName() != null) store.setName(dto.getName());
        if (dto.getInfo() != null) store.setInfo(dto.getInfo());
        if (dto.getCategory() != null) store.setCategory(dto.getCategory());
        if (dto.getOperatingHours() != null) store.setOperatingHours(dto.getOperatingHours());
        if (dto.getPhone() != null) store.setPhone(dto.getPhone());
        if (dto.getAddress() != null) store.setAddress(dto.getAddress());
        if (dto.getOwnerName() != null) store.setOwnerName(dto.getOwnerName());
        if (dto.getLinkUrl() != null) store.setLinkUrl(dto.getLinkUrl());

        if (dto.getRating() != null) {
            validateRating(dto.getRating());
            store.setRating(dto.getRating());
        }

        if (dto.getKeywords() != null) { // 빈 Set 반영 허용 (초기화 목적)
            validateKeywords(dto.getKeywords());
            store.setKeywords(dto.getKeywords());
        }
    }

    // ====== User PATCH 적용 (옵셔널 필드들만) ======
    private void applyUserPatch(User user, StoreRequestDTO dto) {
        if (dto.getUserProfileName() != null) user.setProfileName(dto.getUserProfileName());
        if (dto.getUserName() != null) user.setUserName(dto.getUserName());
        if (dto.getUserEmail() != null) user.setEmail(dto.getUserEmail());
        if (dto.getUserPhone() != null) user.setPhone(dto.getUserPhone());
        if (dto.getUserProfileImage() != null) user.setProfileImage(dto.getUserProfileImage());
        if (dto.getUserUniversity() != null) user.setUniversity(dto.getUserUniversity());

        if (dto.getUserInterest() != null) {
            try {
                Category interest = Category.valueOf(dto.getUserInterest());
                user.setInterest(interest);
            } catch (IllegalArgumentException e) {
                throw new GeneralException(ErrorStatus.INVALID_TYPE,
                        "userInterest 값이 올바르지 않습니다: " + dto.getUserInterest());
            }
        }
    }

    // --- validators ---

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

    private void validateRating(Integer rating) {
        if (rating < 0 || rating > 5) {
            throw new GeneralException(ErrorStatus.INVALID_TYPE, "rating은 0 ~ 5 사이여야 합니다. (" + rating + ")");
        }
    }

    // Not Null 값들 중 중요한 3가지를 골라 이중 검증
    private void ensureRequiredNonNull(Store store) {
        if (isBlank(store.getName()))
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "store name은 필수입니다.");
        if (store.getCategory() == null)
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "store category는 필수입니다.");
        if (isBlank(store.getAddress()))
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING, "store address는 필수입니다.");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
