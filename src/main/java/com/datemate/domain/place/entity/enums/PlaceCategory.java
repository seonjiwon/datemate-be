package com.datemate.domain.place.entity.enums;

/**
 * 장소 카테고리
 * 1. Google Places API의 type을 앱 내부 카테고리로 매핑한다
 * 2. LLM이 코스 설계 시 카테고리 조합의 다양성을 보장하는 데 사용한다
 */
public enum PlaceCategory {

    // 식사류 (한식, 양식, 일식 등)
    RESTAURANT,

    // 카페, 디저트
    CAFE,

    // 체험 활동 (방탈출, 공방, 볼링 등)
    ACTIVITY,

    // 문화 시설 (영화관, 미술관, 공연장)
    CULTURE,

    // 산책, 공원, 야외 장소
    OUTDOOR,

    // 쇼핑 (백화점, 편집숍)
    SHOPPING,

    // 위 카테고리에 해당하지 않는 장소
    ETC
}
