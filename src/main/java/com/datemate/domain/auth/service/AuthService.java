package com.datemate.domain.auth.service;

import com.datemate.domain.auth.code.AuthErrorCode;
import com.datemate.domain.auth.dto.request.LoginRequest;
import com.datemate.domain.auth.dto.request.TokenRefreshRequest;
import com.datemate.domain.auth.dto.response.TokenResponse;
import com.datemate.domain.auth.entity.AuthToken;
import com.datemate.domain.auth.repository.AuthTokenRepository;
import com.datemate.domain.member.entity.Member;
import com.datemate.domain.member.entity.enums.AuthProvider;
import com.datemate.domain.member.repository.MemberRepository;
import com.datemate.global.exception.CustomException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 1. 소셜 로그인 → JWT 발급 → 토큰 갱신 → 로그아웃 흐름을 관리한다
 * 2. JWT 생성/검증은 별도 JwtProvider로 분리할 예정이다 (TODO)
 * 3. 소셜 토큰 검증은 각 플랫폼별 검증 로직으로 분리할 예정이다 (TODO)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthTokenRepository authTokenRepository;

    // TODO: JwtProvider 빈 주입 (JWT 생성/검증 전담)
    // TODO: KakaoAuthClient, AppleAuthClient 빈 주입 (소셜 토큰 검증)

    /**
     * 1. 소셜 토큰으로 사용자를 인증하고 JWT 쌍을 발급한다
     * 2. 신규 사용자면 자동 회원가입한다 (upsert 패턴)
     * 3. 기존 사용자면 프로필을 동기화하고 토큰을 갱신한다
     *
     * @param request 소셜 로그인 요청 (provider, socialToken, deviceInfo)
     * @return JWT access + refresh 토큰 쌍
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 소셜 토큰을 검증하고 사용자 정보를 추출한다
        SocialUserInfo socialUser = verifySocialToken(request.provider(), request.socialToken());

        // 2. 기존 회원 조회 또는 신규 가입
        Member member = memberRepository.findBySocialIdAndAuthProvider(
            socialUser.socialId(), request.provider()
        ).orElseGet(() -> registerNewMember(socialUser, request.provider()));

        // 3. 탈퇴한 회원인지 확인한다
        if (member.getIsDeleted()) {
            throw new CustomException(AuthErrorCode.SOCIAL_TOKEN_INVALID);
        }

        // 4. JWT 토큰 쌍을 생성한다
        // TODO: JwtProvider.generateAccessToken(member)
        String accessToken = "temp-access-token-" + member.getId();
        // TODO: JwtProvider.generateRefreshToken()
        String refreshToken = "temp-refresh-token-" + java.util.UUID.randomUUID();

        // 5. 리프레시 토큰을 DB에 저장한다 (기기별 관리)
        saveOrUpdateRefreshToken(member, refreshToken, request.deviceInfo());

        log.info("[AuthService] 로그인 성공: memberId={}, provider={}", member.getId(), request.provider());

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 1. 리프레시 토큰으로 새 JWT 쌍을 발급한다
     * 2. token rotation — 리프레시 토큰도 함께 갱신하여 탈취 리스크를 줄인다
     *
     * @param request 토큰 갱신 요청 (refreshToken)
     * @return 새 JWT access + refresh 토큰 쌍
     */
    @Transactional
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        // 1. DB에서 리프레시 토큰을 조회한다
        AuthToken authToken = authTokenRepository.findByRefreshToken(request.refreshToken())
            .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 2. 만료 여부를 확인한다
        if (authToken.isExpired()) {
            authTokenRepository.delete(authToken);
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 3. 새 토큰 쌍을 생성한다
        Member member = authToken.getMember();
        // TODO: JwtProvider.generateAccessToken(member)
        String newAccessToken = "temp-access-token-" + member.getId();
        // TODO: JwtProvider.generateRefreshToken()
        String newRefreshToken = "temp-refresh-token-" + java.util.UUID.randomUUID();

        // 4. 리프레시 토큰을 교체한다 (rotation)
        authToken.rotate(newRefreshToken, LocalDateTime.now().plusDays(14));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 1. 로그아웃 처리 — 해당 회원의 모든 리프레시 토큰을 삭제한다
     * 2. 모든 기기에서 동시 로그아웃된다
     */
    @Transactional
    public void logout(Member member) {
        authTokenRepository.deleteAllByMember(member);
        log.info("[AuthService] 로그아웃: memberId={}", member.getId());
    }

    // ============================================================
    // Private: 소셜 인증 (TODO — 실제 SDK 연동 예정)
    // ============================================================

    /**
     * 1. 소셜 플랫폼 서버에 토큰을 검증하고 사용자 정보를 반환한다
     * 2. TODO: 실제 카카오/Apple API 호출로 교체 예정
     */
    private SocialUserInfo verifySocialToken(AuthProvider provider, String socialToken) {
        // TODO: 실제 구현
        // KAKAO → kakaoAuthClient.verify(socialToken) → socialId, nickname, profileImage
        // APPLE → appleAuthClient.verify(socialToken) → socialId, email
        return new SocialUserInfo(
            "social-" + socialToken.hashCode(),
            "DateMate 사용자",
            null
        );
    }

    /**
     * 1. 신규 회원을 등록한다
     * 2. 소셜 플랫폼에서 가져온 프로필로 초기 데이터를 세팅한다
     */
    private Member registerNewMember(SocialUserInfo socialUser, AuthProvider provider) {
        Member member = Member.builder()
            .socialId(socialUser.socialId())
            .authProvider(provider)
            .nickname(socialUser.nickname())
            .profileImageUrl(socialUser.profileImageUrl())
            .build();

        Member savedMember = memberRepository.save(member);
        log.info("[AuthService] 신규 회원 가입: memberId={}, provider={}", savedMember.getId(), provider);

        return savedMember;
    }

    /**
     * 1. 리프레시 토큰을 저장하거나 갱신한다
     * 2. 동일 기기에서 재로그인하면 기존 토큰을 교체한다
     * 3. 새 기기면 새 토큰을 생성한다
     */
    private void saveOrUpdateRefreshToken(Member member, String refreshToken, String deviceInfo) {
        AuthToken authToken = authTokenRepository.findByMemberAndDeviceInfo(member, deviceInfo)
            .orElse(null);

        if (authToken != null) {
            authToken.rotate(refreshToken, LocalDateTime.now().plusDays(14));
        } else {
            AuthToken newToken = AuthToken.builder()
                .member(member)
                .refreshToken(refreshToken)
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
            authTokenRepository.save(newToken);
        }
    }

    /**
     * 소셜 플랫폼에서 추출한 사용자 정보
     * 1. verifySocialToken의 결과를 구조화한다
     * 2. private inner record — AuthService 내부에서만 사용한다
     */
    private record SocialUserInfo(
        String socialId,
        String nickname,
        String profileImageUrl
    ) {
    }
}
