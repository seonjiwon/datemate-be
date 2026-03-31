package com.datemate.domain.member.code;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 회원 도메인 에러 코드
 * 1. 회원 관련 비즈니스 예외를 정의한다
 * 2. 접두사 MEMBER로 도메인을 식별한다
 */
@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    // 회원 조회 실패
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_0", "존재하지 않는 회원입니다."),

    // 이미 탈퇴한 회원
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.GONE, "MEMBER410_0", "이미 탈퇴한 회원입니다."),

    // 소셜 계정 중복 가입 시도 (동일 provider + socialId)
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER409_0", "이미 가입된 소셜 계정입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
