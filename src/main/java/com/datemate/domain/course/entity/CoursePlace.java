package com.datemate.domain.course.entity;

import com.datemate.domain.course.entity.enums.TravelMethod;
import com.datemate.domain.place.entity.Place;
import com.datemate.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스-장소 연결 엔티티 (조인 테이블 + 추가 속성)
 * 1. Course와 Place의 다대다 관계를 풀어낸 중간 테이블이다
 * 2. 방문 순서, 체류 시간, 이동 수단 등 코스 맥락 정보를 포함한다
 * 3. order_index로 정렬하면 타임라인 순서가 된다
 */
@Entity
@Table(name = "course_place", indexes = {
    @Index(name = "idx_course_place_course", columnList = "course_id"),
    @Index(name = "idx_course_place_order", columnList = "course_id, order_index")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoursePlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_place_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    // 소속 코스
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    // 연결된 장소 — Google Places API에서 가져온 실제 장소
    private Place place;

    @Column(name = "order_index", nullable = false)
    // 코스 내 방문 순서 (0-based) — 타임라인 렌더링에 사용
    private Integer orderIndex;

    @Column(name = "duration_minutes", nullable = false)
    // 이 장소 예상 체류 시간 (분) — LLM이 카테고리 기반으로 추정
    private Integer durationMinutes;

    @Column(name = "cost_min")
    // 이 장소 예상 최소 비용 (원)
    private Integer costMin;

    @Column(name = "cost_max")
    // 이 장소 예상 최대 비용 (원)
    private Integer costMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_method_to_next", length = 10)
    // 다음 장소까지 이동 수단 — 마지막 장소는 NONE
    @Builder.Default
    private TravelMethod travelMethodToNext = TravelMethod.NONE;

    @Column(name = "travel_time_to_next")
    // 다음 장소까지 이동 시간 (분) — 마지막 장소는 null
    private Integer travelTimeToNext;

    @Column(name = "memo", length = 300)
    // LLM이 생성한 장소별 추천 이유 또는 팁 (ex: "2층 창가석 추천")
    private String memo;
}
