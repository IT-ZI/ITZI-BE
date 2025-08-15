package com.itzi.itzi.global.exception;

import com.itzi.itzi.global.api.code.BaseErrorCode;
import com.itzi.itzi.global.api.dto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final String detail;

    // 기본: 코드의 기본 메시지를 RuntimeException에 올려서 null 방지
    public GeneralException(BaseErrorCode errorCode) {
        super(errorCode.getReason().getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public GeneralException(BaseErrorCode errorCode, String detail) {
        super(buildMessage(errorCode, detail));
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public GeneralException(BaseErrorCode errorCode, String detail, Throwable cause) {
        super(buildMessage(errorCode, detail), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    private static String buildMessage(BaseErrorCode errorCode, String detail) {
        String base = errorCode.getReason().getMessage();
        return (detail == null || detail.isBlank()) ? base : base + ": " + detail;
    }

    public ErrorReasonDto getErrorReason() {
        return this.errorCode.getReason();
    }

    public ErrorReasonDto getErrorReasonHttpStatus() {
        return this.errorCode.getReasonHttpStatus();
    }
}