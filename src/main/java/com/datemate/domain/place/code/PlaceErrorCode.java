package com.datemate.domain.place.code;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 장소 도메인 에러 코드
 * 1. Google Places API 연동 및 장소 조회 관련 예외를 정의한다
 * 2. 접두사 PLACE로 도메인을 식별한다
 */
@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseErrorCode {

    // 장소 조회 실패
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_0", "존재하지 않는 장소입니다."),

    // Google Places API 호출 실패
    GOOGLE_API_ERROR(HttpStatus.BAD_GATEWAY, "PLACE502_0", "Google Places API 호출에 실패했습니다."),

    // Google Places API 응답 파싱 실패
    GOOGLE_API_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "PLACE502_1", "Google Places API 응답을 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
