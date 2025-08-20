package com.itzi.itzi.global.exception;

import com.itzi.itzi.global.api.code.ErrorStatus;

public class NotFoundException extends GeneralException {
    public NotFoundException() {
        super(ErrorStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorStatus.NOT_FOUND, message);
    }
}
