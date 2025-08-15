package com.itzi.itzi.global.api.code;

import com.itzi.itzi.global.api.dto.ErrorReasonDto;

public interface BaseErrorCode {

    ErrorReasonDto getReason();
    ErrorReasonDto getReasonHttpStatus();

}