package com.itzi.itzi.global.api.code;

import com.itzi.itzi.global.api.dto.ReasonDto;

public interface BaseCode {

    ReasonDto getReason();                  // 메시지 + 코드 반환
    ReasonDto getReasonHttpStatus();        // 메시지 + Http 상태 반환
}
