package com.datemate.domain.course.code;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 코스 도메인 에러 코드
 * 1. 코스 생성, 조회, 공유 관련 예외를 정의한다
 * 2. 접두사 COURSE로 도메인을 식별한다
 */
@Getter
@AllArgsConstructor
public enum CourseErrorCode implements BaseErrorCode {

    // 코스 조회 실패
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_0", "존재하지 않는 코스입니다."),

    // 코스 요청 조회 실패
    COURSE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_1", "존재하지 않는 코스 요청입니다."),

    // LLM 코스 생성 실패 (Gemini API 오류, 파싱 실패 등)
    COURSE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COURSE500_0", "AI 코스 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // LLM 에이전트 루프 최대 횟수 초과
    COURSE_GENERATION_LOOP_EXCEEDED(HttpStatus.INTERNAL_SERVER_ERROR, "COURSE500_1", "코스 생성 처리 시간이 초과되었습니다."),

    // 코스 확정 불가 (이미 확정 또는 완료된 상태)
    COURSE_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "COURSE409_0", "이미 확정된 코스입니다."),

    // 공유 토큰 조회 실패
    SHARE_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_2", "유효하지 않은 공유 링크입니다."),

    // 공유 링크 만료
    SHARE_TOKEN_EXPIRED(HttpStatus.GONE, "COURSE410_0", "만료된 공유 링크입니다."),

    // 코스 소유자가 아닌 사용자의 접근
    COURSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COURSE403_0", "해당 코스에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
