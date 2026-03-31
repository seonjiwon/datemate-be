package com.datemate.domain.course.entity.enums;

/**
 * 데이트 분위기
 * 1. 사용자가 코스 생성 시 선택하는 선호 분위기이다
 * 2. LLM 프롬프트에 전달되어 장소 선택 기준으로 작용한다
 */
public enum Mood {

    // 조용하고 차분한 데이트 (카페, 미술관, 산책)
    QUIET,

    // 활동적인 데이트 (방탈출, 볼링, 놀이공원)
    ACTIVE,

    // 로맨틱한 데이트 (레스토랑, 야경, 와인바)
    ROMANTIC,

    // 가벼운 데이트 (맛집 탐방, 쇼핑, 영화)
    CASUAL
}
