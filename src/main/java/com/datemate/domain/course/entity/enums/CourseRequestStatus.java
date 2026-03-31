package com.datemate.domain.course.entity.enums;

/**
 * 코스 생성 요청 처리 상태
 * 1. LLM + Places API 파이프라인의 진행 상태를 추적한다
 * 2. 비동기 처리 시 클라이언트에서 폴링하는 기준값이 된다
 */
public enum CourseRequestStatus {

    // 요청 접수됨 — LLM 처리 대기 중
    PENDING,

    // LLM이 코스 구조를 생성 중
    PROCESSING,

    // 코스 생성 완료 — 결과 조회 가능
    COMPLETED,

    // 처리 실패 (LLM 오류, API 한도 초과 등)
    FAILED
}
