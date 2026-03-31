package com.datemate.global.exception;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.Getter;

/**
 * 커스텀 예외 클래스
 * 1. BaseErrorCode를 감싸서 도메인별 에러를 표현한다
 * 2. GlobalExceptionHandler에서 이 예외를 잡아 CustomResponse로 변환한다
 */
@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode code;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode;
    }
}
