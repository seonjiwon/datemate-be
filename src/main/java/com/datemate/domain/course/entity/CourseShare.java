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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 공유 엔티티
 * 1. 확정된 코스를 상대방에게 공유할 때 사용하는 일회성 토큰을 관리한다
 * 2. 딥링크 URL에 share_token을 포함하여 비회원도 코스를 조회할 수 있다
 * 3. 만료 시간과 조회 수를 트래킹하여 남용을 방지한다
 */
@Entity
@Table(name = "course_share", indexes = {
    @Index(name = "idx_course_share_token", columnList = "share_token", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseShare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_share_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    // 공유 대상 코스 — 하나의 코스에 하나의 공유 링크만 존재
    private Course course;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    // 공유용 고유 토큰 (UUID 기반) — URL 파라미터로 전달
    private String shareToken;

    @Column(name = "expires_at", nullable = false)
    // 공유 링크 만료 시각 — 기본 72시간, 이후 접근 불가
    private LocalDateTime expiresAt;

    @Column(name = "view_count", nullable = false)
    // 공유 링크 조회 횟수 — 분석 및 남용 탐지용
    @Builder.Default
    private Integer viewCount = 0;

    // === 비즈니스 메서드 ===

    /**
     * 1. 조회 수를 1 증가시킨다
     * 2. 공유 링크를 통해 코스를 볼 때마다 호출된다
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 1. 공유 링크 만료 여부를 판단한다
     * 2. 만료 시 클라이언트에 적절한 에러를 반환하는 기준이 된다
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 1. 공유 링크 유효성을 검증한다
     * 2. 만료되지 않았으면 true — 컨트롤러에서 분기 처리에 사용
     */
    public boolean isValid() {
        return !isExpired();
    }
}
