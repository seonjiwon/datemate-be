package com.datemate.domain.course.dto.response;

import com.datemate.domain.course.entity.CoursePlace;
import com.datemate.domain.course.entity.enums.TravelMethod;
import com.datemate.domain.place.entity.Place;
import java.math.BigDecimal;

/**
 * 코스 내 개별 장소 응답 DTO
 * 1. 타임라인의 한 칸을 표현한다 (장소 정보 + 이동 정보)
 * 2. CoursePlace + Place 엔티티를 합쳐서 플랫하게 반환한다
 *
 * @param orderIndex 방문 순서 (0-based)
 * @param placeName 장소명
 * @param address 주소
 * @param latitude 위도
 * @param longitude 경도
 * @param category 장소 카테고리
 * @param rating 평점
 * @param durationMinutes 체류 시간 (분)
 * @param costMin 최소 비용 (원)
 * @param costMax 최대 비용 (원)
 * @param travelMethodToNext 다음 장소까지 이동 수단
 * @param travelTimeToNext 다음 장소까지 이동 시간 (분)
 * @param memo 추천 이유 또는 팁
 * @param photoReference Google 사진 참조 키
 */
public record CoursePlaceResponse(
    Integer orderIndex,
    String placeName,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String category,
    BigDecimal rating,
    Integer durationMinutes,
    Integer costMin,
    Integer costMax,
    TravelMethod travelMethodToNext,
    Integer travelTimeToNext,
    String memo,
    String photoReference
) {

    /**
     * 1. CoursePlace와 Place 엔티티에서 응답 DTO를 생성한다
     * 2. 두 엔티티의 필드를 플랫하게 합쳐 프론트엔드가 바로 사용할 수 있게 한다
     */
    public static CoursePlaceResponse from(CoursePlace coursePlace, Place place) {
        return new CoursePlaceResponse(
            coursePlace.getOrderIndex(),
            place.getName(),
            place.getAddress(),
            place.getLatitude(),
            place.getLongitude(),
            place.getCategory().name(),
            place.getRating(),
            coursePlace.getDurationMinutes(),
            coursePlace.getCostMin(),
            coursePlace.getCostMax(),
            coursePlace.getTravelMethodToNext(),
            coursePlace.getTravelTimeToNext(),
            coursePlace.getMemo(),
            place.getPhotoReference()
        );
    }
}
