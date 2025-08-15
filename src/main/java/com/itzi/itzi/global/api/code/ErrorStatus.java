package com.itzi.itzi.global.api.code;

import com.itzi.itzi.global.api.dto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E-500-01", "서버 내부 오류가 발생했습니다"),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "E-400-01", "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "E-400-02", "요청 값이 올바르지 않습니다."),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "E-400-03", "필수 입력 항목이 누락되었습니다."),
    DATE_RANGE_INVALID(HttpStatus.BAD_REQUEST, "E-400-04", "종료일은 시작일보다 빠를 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E-404", "대상을 찾을 수 없습니다."),
    ALREADY_PUBLISHED(HttpStatus.CONFLICT, "E-409-01", "이미 게시된 글입니다."),

    // Gemini
    GEMINI_BLOCKED(HttpStatus.BAD_REQUEST, "A-400-01", "Gemini 응답이 정책에 의해 차단되었습니다."),
    GEMINI_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "A-500-01", "Gemini API 키가 설정되지 않았습니다."),
    GEMINI_HTTP_ERROR(HttpStatus.BAD_GATEWAY, "A-502-01", "Gemini HTTP 호출 실패"),
    GEMINI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "A-502-02", "Gemini 응답 포맷이 올바르지 않습니다."),
    GEMINI_ERROR_RETURNED(HttpStatus.BAD_GATEWAY, "A-502-03", "Gemini 응답 에러"),
    GEMINI_EMPTY_TEXT(HttpStatus.BAD_GATEWAY, "A-502-04", "Gemini가 비어있는 텍스트를 반환했습니다");

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
