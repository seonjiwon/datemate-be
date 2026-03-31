package com.datemate.domain.course.entity.enums;

/**
 * 이동 수단 선호
 * 1. 코스 내 장소 간 이동 수단 선호를 나타낸다
 * 2. LLM이 장소 간 거리와 이동 가능성을 판단하는 기준이 된다
 */
public enum Transport {

    // 도보 위주 (장소 간 도보 15분 이내)
    WALK,

    // 대중교통 이용 (버스, 지하철)
    PUBLIC,

    // 자차 또는 택시 이용
    CAR
}
