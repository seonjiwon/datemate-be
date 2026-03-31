package com.datemate.domain.course.entity;

import com.datemate.domain.course.entity.enums.CourseStatus;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이트 코스 엔티티
 * 1. LLM이 설계하고 Places API로 채워진 최종 코스 결과물이다
 * 2. CourseRequest 1개 → Course N개 (여러 코스 옵션 제공 가능)
 * 3. CONFIRMED 상태에서만 공유/실행 기능이 활성화된다
 */
@Entity
@Table(name = "course", indexes = {
    @Index(name = "idx_course_member", columnList = "member_id"),
    @Index(name = "idx_course_request", columnList = "course_request_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_request_id", nullable = false)
    // 이 코스를 생성한 요청 — 역추적 및 재생성에 사용
    private CourseRequest courseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    // 코스 소유자 — CourseRequest.member와 동일하지만 조회 편의를 위해 비정규화
    private Member member;

    @Column(name = "title", nullable = false, length = 100)
    // 코스 제목 (LLM이 생성, ex: "홍대 감성 카페 투어")
    private String title;

    @Column(name = "description", length = 500)
    // 코스 설명 (LLM이 생성하는 한 줄 요약)
    private String description;

    @Column(name = "total_duration", nullable = false)
    // 총 소요 시간 (분) — 각 장소 체류 시간 + 이동 시간의 합
    private Integer totalDuration;

    @Column(name = "total_cost_min", nullable = false)
    // 예상 최소 비용 (원) — 각 장소 최소 비용의 합
    private Integer totalCostMin;

    @Column(name = "total_cost_max", nullable = false)
    // 예상 최대 비용 (원) — 각 장소 최대 비용의 합
    private Integer totalCostMax;

    @Column(name = "start_time", length = 5)
    // 코스 시작 시각 (HH:mm 형식, ex: "14:00") — null이면 시간 미정
    private String startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    // 코스 확정 상태 (DRAFT, CONFIRMED, COMPLETED)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    // === 비즈니스 메서드 ===

    /**
     * 1. 사용자가 코스를 확정한다
     * 2. DRAFT → CONFIRMED 전환, 이후 공유 기능 사용 가능
     */
    public void confirm() {
        this.status = CourseStatus.CONFIRMED;
    }

    /**
     * 1. 데이트 완료 후 상태를 변경한다
     * 2. v1.1에서 리뷰 작성 트리거로 사용된다
     */
    public void complete() {
        this.status = CourseStatus.COMPLETED;
    }
}
