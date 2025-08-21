package com.itzi.itzi.global.api.code;

import com.itzi.itzi.global.api.dto.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    PROFILE_UPDATED(HttpStatus.OK, "PROFILE200", "프로필이 성공적으로 수정되었습니다."),
    PARTNERSHIP_POST(HttpStatus.OK, "POSTINQUIRY200", "문의 글이 중간 저장되었습니다."),
    PARTNERSHIP_SENT(HttpStatus.OK, "INQUIRY200", "문의가 성공적으로 전송되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason(){
        return ReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(status)
                .build();
    }
}
