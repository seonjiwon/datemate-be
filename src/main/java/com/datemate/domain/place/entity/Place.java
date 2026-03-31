package com.datemate.domain.place.entity;

import com.datemate.domain.place.entity.enums.PlaceCategory;
import com.datemate.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 엔티티
 * 1. Google Places API로 조회한 장소 정보를 캐싱한다
 * 2. 동일 장소의 반복 API 호출을 방지하여 비용을 절감한다
 * 3. google_place_id를 유니크 키로 사용하여 중복 저장을 막는다
 */
@Entity
@Table(name = "place", indexes = {
    @Index(name = "idx_place_google_id", columnList = "google_place_id", unique = true),
    @Index(name = "idx_place_category", columnList = "category")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @Column(name = "google_place_id", nullable = false, unique = true, length = 200)
    // Google Places API가 발급한 고유 장소 ID — 외부 시스템 연동 키
    private String googlePlaceId;

    @Column(name = "name", nullable = false, length = 100)
    // 장소명 (ex: "스타벅스 강남점")
    private String name;

    @Column(name = "address", nullable = false, length = 300)
    // 도로명 또는 지번 주소
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    // 위도 — 소수점 7자리까지 저장 (약 1cm 정밀도)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    // 경도 — 소수점 7자리까지 저장
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    // 앱 내부 카테고리 — Google type에서 매핑
    private PlaceCategory category;

    @Column(name = "rating", precision = 2, scale = 1)
    // Google 사용자 평점 (0.0 ~ 5.0) — null이면 평점 정보 없음
    private BigDecimal rating;

    @Column(name = "price_level")
    // Google 가격대 수준 (0~4) — 0: 무료, 4: 매우 비쌈, null이면 정보 없음
    private Integer priceLevel;

    @Column(name = "photo_reference", length = 500)
    // Google Place Photo 참조 키 — 이미지 URL 생성에 사용
    private String photoReference;

    // === 비즈니스 메서드 ===

    /**
     * 1. Google API 재조회 시 장소 정보를 최신화한다
     * 2. 평점, 가격대, 사진은 시간이 지나면 변할 수 있으므로 갱신 대상이다
     */
    public void updateFromGoogle(BigDecimal rating, Integer priceLevel, String photoReference) {
        this.rating = rating;
        this.priceLevel = priceLevel;
        this.photoReference = photoReference;
    }
}
