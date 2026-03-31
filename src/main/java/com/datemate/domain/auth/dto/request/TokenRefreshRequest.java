package com.datemate.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO
 * 1. 만료된 access token을 refresh token으로 재발급할 때 사용한다
 *
 * @param refreshToken 클라이언트가 보관 중인 리프레시 토큰
 */
public record TokenRefreshRequest(
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    String refreshToken
) {
}
