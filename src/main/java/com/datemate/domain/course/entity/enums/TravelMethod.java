package com.datemate.domain.course.entity.enums;

/**
 * 장소 간 실제 이동 방법
 * 1. 코스 내 장소 → 다음 장소로 이동할 때의 구체적 수단이다
 * 2. Transport(선호)와 달리, 각 구간별 실제 이동 방법을 나타낸다
 */
public enum TravelMethod {

    // 도보 이동
    WALK,

    // 버스 이용
    BUS,

    // 지하철 이용
    SUBWAY,

    // 택시 이용
    TAXI,

    // 마지막 장소 — 다음 이동 없음
    NONE
}
