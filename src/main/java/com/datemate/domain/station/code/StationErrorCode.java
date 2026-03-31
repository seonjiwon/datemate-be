package com.datemate.domain.station.code;

import com.datemate.global.code.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 중간역 도메인 에러 코드
 * 1. ODsay API 연동 및 중간역 추천 관련 예외를 정의한다
 * 2. 접두사 STATION으로 도메인을 식별한다
 */
@Getter
@AllArgsConstructor
public enum StationErrorCode implements BaseErrorCode {

    // 중간역 후보가 하나도 없음 (두 출발지 사이에 대중교통 경로 없음 등)
    NO_STATION_CANDIDATES(HttpStatus.NOT_FOUND, "STATION404_0", "중간역 후보를 찾을 수 없습니다. 출발지를 확인해주세요."),

    // ODsay API 호출 실패
    ODSAY_API_ERROR(HttpStatus.BAD_GATEWAY, "STATION502_0", "대중교통 API 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
