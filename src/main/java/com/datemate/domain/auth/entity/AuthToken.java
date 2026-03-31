package com.datemate.domain.auth.entity;

import com.datemate.domain.member.entity.Member;
import com.datemate.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 저장 엔티티
 * 1. JWT refresh token을 서버 측에서 관리한다
 * 2. 디바이스별 토큰을 분리하여 다중 기기 로그인을 지원한다
 * 3. 로그아웃 시 해당 토큰 row를 삭제하여 무효화한다
 */
@Entity
@Table(name = "auth_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    // 토큰 소유 회원 — 한 회원이 여러 디바이스에서 로그인 가능하므로 ManyToOne
    private Member member;

    @Column(name = "refresh_token", nullable = false, length = 500)
    // JWT refresh token 값 — access token 만료 시 재발급에 사용
    private String refreshToken;

    @Column(name = "device_info", length = 200)
    // 로그인한 기기 정보 (ex: "iPhone 15 Pro", "Galaxy S24") — 다중 기기 관리용
    private String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    // 리프레시 토큰 만료 시각 — 이 시각이 지나면 재로그인 필요
    private LocalDateTime expiresAt;

    // === 비즈니스 메서드 ===

    /**
     * 1. 리프레시 토큰을 갱신한다
     * 2. token rotation 전략 — 사용할 때마다 새 토큰으로 교체하여 탈취 리스크를 줄인다
     */
    public void rotate(String newRefreshToken, LocalDateTime newExpiresAt) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
    }

    /**
     * 1. 토큰 만료 여부를 판단한다
     * 2. 만료되었으면 true를 반환하여 재로그인을 유도한다
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
