package com.datemate.domain.course.entity;

import com.datemate.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 중간역 후보 엔티티
 * 1. ODsay API로 계산된 A, B 양측의 대중교통 중간 지점 후보를 저장한다
 * 2. 하나의 CourseRequest에 3~5개 후보가 생성된다
 * 3. 사용자가 하나를 선택하면 CourseRequest.selectedStation에 반영된다
 */
@Entity
@Table(name = "station_candidate", indexes = {
    @Index(name = "idx_station_candidate_request", columnList = "course_request_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StationCandidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_candidate_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_request_id", nullable = false)
    // 소속 코스 요청 — 어떤 요청의 후보인지
    private CourseRequest courseRequest;

    @Column(name = "station_name", nullable = false, length = 50)
    // 역 이름 (ex: "강남역", "홍대입구역")
    private String stationName;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    // 역 위도
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    // 역 경도
    private BigDecimal longitude;

    @Column(name = "travel_time_from_a", nullable = false)
    // A 출발지에서 이 역까지 대중교통 소요 시간 (분)
    private Integer travelTimeFromA;

    @Column(name = "travel_time_from_b", nullable = false)
    // B 출발지에서 이 역까지 대중교통 소요 시간 (분)
    private Integer travelTimeFromB;

    @Column(name = "is_selected", nullable = false)
    // 사용자가 이 후보를 최종 선택했는지 여부
    @Builder.Default
    private Boolean isSelected = false;

    // === 비즈니스 메서드 ===

    /**
     * 1. 이 후보를 선택 상태로 변경한다
     * 2. 같은 CourseRequest의 다른 후보들은 서비스 레이어에서 일괄 해제한다
     */
    public void select() {
        this.isSelected = true;
    }

    /**
     * 1. 선택을 해제한다
     * 2. 다른 후보 선택 시 기존 선택을 취소할 때 사용한다
     */
    public void deselect() {
        this.isSelected = false;
    }

    /**
     * 1. A, B 양측 소요 시간의 합을 계산한다
     * 2. 중간역 후보 정렬 기준으로 사용 — 합이 작을수록 양쪽 모두에게 공평하다
     */
    public int getTotalTravelTime() {
        return this.travelTimeFromA + this.travelTimeFromB;
    }
}
