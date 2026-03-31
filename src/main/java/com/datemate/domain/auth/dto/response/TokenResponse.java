package com.datemate.domain.auth.dto.response;

/**
 * 토큰 발급 응답 DTO
 * 1. 로그인 성공 또는 토큰 갱신 시 JWT 쌍을 반환한다
 * 2. accessToken은 짧은 수명 (15분), refreshToken은 긴 수명 (14일)
 *
 * @param accessToken API 호출용 JWT — Authorization 헤더에 Bearer로 전달
 * @param refreshToken access token 재발급용 — SecureStore에 안전하게 보관
 */
public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}
