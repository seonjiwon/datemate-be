package com.datemate.domain.auth.repository;

import com.datemate.domain.auth.entity.AuthToken;
import com.datemate.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 인증 토큰 저장소
 * 1. 리프레시 토큰 기반 재발급과 로그아웃 처리에 사용한다
 * 2. 회원+디바이스 단위로 토큰을 관리한다
 */
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * 1. 리프레시 토큰 값으로 토큰 엔티티를 조회한다
     * 2. 토큰 재발급(rotation) 시 기존 토큰을 찾아 갱신하는 데 사용한다
     */
    Optional<AuthToken> findByRefreshToken(String refreshToken);

    /**
     * 1. 회원의 모든 토큰을 삭제한다
     * 2. 전체 기기 로그아웃 시 사용한다
     */
    void deleteAllByMember(Member member);

    /**
     * 1. 회원 + 디바이스 조합으로 토큰을 조회한다
     * 2. 동일 기기 재로그인 시 기존 토큰을 갱신하는 데 사용한다
     */
    Optional<AuthToken> findByMemberAndDeviceInfo(Member member, String deviceInfo);
}
