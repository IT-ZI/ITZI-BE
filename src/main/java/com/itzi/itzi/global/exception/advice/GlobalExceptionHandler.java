package com.itzi.itzi.global.exception.advice;

import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.api.dto.ApiResponse;
import com.itzi.itzi.global.api.dto.ErrorReasonDto;
import com.itzi.itzi.global.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // GeneralException 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException
    (GeneralException e) {
        ErrorReasonDto reason = e.getErrorReasonHttpStatus();
        return new ResponseEntity<>(ApiResponse.onFailure(reason.getCode
                (), reason.getMessage(), null), reason.getHttpStatus());
    }

    // @Valid 유효성 검사 실패 처리
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return new ResponseEntity<>(ApiResponse.onFailure(ErrorStatus.VALIDATION_ERROR.getCode(), "Validation Error", errors),
                HttpStatus.BAD_REQUEST);
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        e.printStackTrace(); // 서버 로그에 예외 내용 출력
        return new ResponseEntity<>(ApiResponse.onFailure(ErrorStatus._BAD_REQUEST.getCode(), e.getMessage(), null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
