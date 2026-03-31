package com.datemate.global.exception;

import io.github.seonjiwon.code_combine.global.code.error.BaseErrorCode;
import lombok.Getter;


@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode code;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode;
    }
}
