package com.datemate.domain.course.entity;

import com.datemate.domain.course.entity.enums.CourseRequestStatus;
import com.datemate.domain.course.entity.enums.Mood;
import com.datemate.domain.course.entity.enums.Transport;
import com.datemate.domain.member.entity.Member;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 생성 요청 엔티티
 * 1. 사용자가 입력한 출발지, 예산, 분위기 등 조건을 저장한다
 * 2. LLM 파이프라인의 입력값이자 요청 이력 역할을 한다
 * 3. 하나의 요청에서 여러 중간역 후보(StationCandidate)가 생성된다
 */
@Entity
@Table(name = "course_request", indexes = {
    @Index(name = "idx_course_request_member", columnList = "member_id"),
    @Index(name = "idx_course_request_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_request_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    // 요청한 회원 — 로그인한 사용자
    private Member member;

    // === A의 출발지 (커플 중 한 명) ===

    @Column(name = "origin_a_address", nullable = false, length = 300)
    // A의 출발 주소 (사용자 입력 또는 Geocoding 결과)
    private String originAAddress;

    @Column(name = "origin_a_lat", nullable = false, precision = 10, scale = 7)
    // A의 출발지 위도
    private BigDecimal originALat;

    @Column(name = "origin_a_lng", nullable = false, precision = 10, scale = 7)
    // A의 출발지 경도
    private BigDecimal originALng;

    // === B의 출발지 (커플 중 다른 한 명) ===

    @Column(name = "origin_b_address", nullable = false, length = 300)
    // B의 출발 주소
    private String originBAddress;

    @Column(name = "origin_b_lat", nullable = false, precision = 10, scale = 7)
    // B의 출발지 위도
    private BigDecimal originBLat;

    @Column(name = "origin_b_lng", nullable = false, precision = 10, scale = 7)
    // B의 출발지 경도
    private BigDecimal originBLng;

    // === 사용자가 선택한 중간역 ===

    @Column(name = "selected_station_name", length = 50)
    // 선택된 중간역 이름 — null이면 아직 선택 전
    private String selectedStationName;

    @Column(name = "selected_station_lat", precision = 10, scale = 7)
    // 선택된 중간역 위도
    private BigDecimal selectedStationLat;

    @Column(name = "selected_station_lng", precision = 10, scale = 7)
    // 선택된 중간역 경도
    private BigDecimal selectedStationLng;

    // === 데이트 조건 ===

    @Column(name = "budget_min", nullable = false)
    // 최소 예산 (원) — 프론트에서 BUDGET_PRESETS로 선택
    private Integer budgetMin;

    @Column(name = "budget_max", nullable = false)
    // 최대 예산 (원)
    private Integer budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood", nullable = false, length = 15)
    // 선호 분위기 (QUIET, ACTIVE, ROMANTIC, CASUAL)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport", nullable = false, length = 10)
    // 선호 이동 수단 (WALK, PUBLIC, CAR)
    private Transport transport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    // 요청 처리 상태 — LLM 파이프라인 진행 추적용
    @Builder.Default
    private CourseRequestStatus status = CourseRequestStatus.PENDING;

    // === 비즈니스 메서드 ===

    /**
     * 1. 사용자가 중간역을 선택했을 때 호출한다
     * 2. StationCandidate 목록에서 선택된 역 정보를 이 엔티티에 기록한다
     */
    public void selectStation(String name, BigDecimal lat, BigDecimal lng) {
        this.selectedStationName = name;
        this.selectedStationLat = lat;
        this.selectedStationLng = lng;
    }

    /**
     * 1. 요청 상태를 변경한다
     * 2. LLM 파이프라인의 각 단계에서 호출하여 진행 상태를 추적한다
     */
    public void updateStatus(CourseRequestStatus newStatus) {
        this.status = newStatus;
    }
}
