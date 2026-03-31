package com.datemate.domain.member.entity;

import com.datemate.domain.member.entity.enums.AuthProvider;
import com.datemate.domain.member.entity.enums.UserRole;
import com.datemate.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티
 * 1. 소셜 로그인 기반 사용자 정보를 관리한다
 * 2. soft delete 패턴으로 GDPR/개인정보보호법 대응한다
 * 3. socialId + authProvider 조합이 유니크 식별자 역할을 한다
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    // PK — AUTO_INCREMENT
    private Long id;

    @Column(name = "email", length = 100)
    // 소셜 계정에서 가져온 이메일 (Apple은 비공개 릴레이 주소일 수 있음)
    private String email;

    @Column(name = "nickname", nullable = false, length = 30)
    // 앱 내 표시 이름 (최초 가입 시 소셜 프로필에서 자동 세팅)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    // 프로필 이미지 URL (소셜 프로필 사진 또는 사용자 업로드)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 10)
    // 인증 제공자 (KAKAO, APPLE) — 동일인이 두 수단으로 가입하면 별도 계정
    private AuthProvider authProvider;

    @Column(name = "social_id", nullable = false, length = 100)
    // 소셜 플랫폼에서 발급한 고유 사용자 ID
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    // 사용자 권한 (USER, ADMIN)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_deleted", nullable = false)
    // soft delete 플래그 — true면 탈퇴한 사용자
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    // 탈퇴 처리 시각 — GDPR 데이터 보존 기한 계산에 사용
    private LocalDateTime deletedAt;

    // === 비즈니스 메서드 ===

    /**
     * 1. 프로필 정보를 갱신한다
     * 2. 소셜 로그인 시 최신 정보로 동기화할 때 사용한다
     */
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 1. soft delete를 수행한다
     * 2. 실제 DB row는 남기고 is_deleted 플래그만 변경한다
     * 3. 30일 후 배치로 물리 삭제하는 정책을 전제한다
     */
    public void withdraw() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.email = null;
        this.nickname = "탈퇴한 사용자";
        this.profileImageUrl = null;
    }
}
