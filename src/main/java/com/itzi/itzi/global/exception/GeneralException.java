package com.itzi.itzi.global.exception;

import com.itzi.itzi.global.api.code.BaseErrorCode;
import com.itzi.itzi.global.api.dto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private final BaseErrorCode errorCode;
    public ErrorReasonDto getErrorReason() {
        return this.errorCode.getReason();
    }

    public ErrorReasonDto getErrorReasonHttpStatus() {
        return this.errorCode.getReasonHttpStatus();
    }
}
