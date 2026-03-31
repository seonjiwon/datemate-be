package com.datemate.domain.place.repository;

import com.datemate.domain.place.entity.Place;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 장소 저장소
 * 1. Google Places API 결과 캐싱 및 중복 방지에 사용한다
 * 2. google_place_id로 기존 캐싱 여부를 확인한다
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 1. Google Place ID로 기존 저장된 장소를 조회한다
     * 2. 이미 저장된 장소면 API 재호출 없이 캐시를 사용한다
     */
    Optional<Place> findByGooglePlaceId(String googlePlaceId);

    /**
     * 1. 해당 Google Place ID의 장소가 이미 캐싱되어 있는지 확인한다
     * 2. 캐싱 여부에 따라 API 호출 or DB 조회를 분기한다
     */
    boolean existsByGooglePlaceId(String googlePlaceId);
}
