package com.datemate.domain.auth.code;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증 도메인 에러 코드
 * 1. JWT 발급, 갱신, 검증 관련 예외를 정의한다
 * 2. 접두사 AUTH로 도메인을 식별한다
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 소셜 로그인 토큰 검증 실패 (카카오/Apple 서버에서 거부)
    SOCIAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH401_0", "소셜 인증 토큰이 유효하지 않습니다."),

    // 리프레시 토큰이 DB에 존재하지 않음 (이미 로그아웃되었거나 탈취 의심)
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH401_1", "리프레시 토큰을 찾을 수 없습니다."),

    // 리프레시 토큰 만료
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_2", "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요."),

    // 지원하지 않는 인증 제공자 (KAKAO, APPLE 외)
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_0", "지원하지 않는 로그인 방식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
