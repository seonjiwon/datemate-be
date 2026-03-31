package com.datemate.domain.course.entity.enums;

/**
 * 코스 확정 상태
 * 1. 사용자가 AI 추천 결과를 수락하는 과정을 추적한다
 * 2. CONFIRMED 상태에서만 공유 기능이 활성화된다
 */
public enum CourseStatus {

    // AI가 생성한 초안 상태 — 아직 사용자가 확인하지 않음
    DRAFT,

    // 사용자가 수락하여 확정된 코스
    CONFIRMED,

    // 데이트 완료 후 상태 (v1.1 리뷰 기능과 연계)
    COMPLETED
}
