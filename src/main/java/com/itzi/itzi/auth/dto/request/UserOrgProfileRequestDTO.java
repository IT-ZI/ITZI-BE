package com.itzi.itzi.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.itzi.itzi.auth.domain.OrgType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

// 직렬화(자바 객체->JSON으로 변환) 시, null 값은 빼겠다는 조건
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserOrgProfileRequestDTO(

        // User (필수)
        @NotBlank String profileName,
        @NotBlank String userName,
        @Email String email,
        @NotBlank String phone,
        @NotBlank String profileImage,
        @NotBlank String university,
        String interest,

        // OrgProfile (필수)
        @NotNull OrgType orgType,
        @NotBlank String schoolName,

        // OrgProfile (선택)
        String unitName,
        String orgPhone,
        String address,
        String ownerName,
        String linkUrl,
        String intro,
        Set<String> keywords
) {}
