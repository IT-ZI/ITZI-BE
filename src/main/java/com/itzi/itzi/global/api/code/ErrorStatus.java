package com.itzi.itzi.global.api.code;

import com.itzi.itzi.global.api.dto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E-001", "서버 내부 오류가 발생했습니다"),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "E-002", "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "E-003", "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E-004", "대상을 찾을 수 없습니다."),

    // Gemini
    GEMINI_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "A-500", "Gemini API 키가 설정되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason(){
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus(){
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }


}
