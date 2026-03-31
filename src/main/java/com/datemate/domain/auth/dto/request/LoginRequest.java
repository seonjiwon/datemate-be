package com.datemate.domain.auth.dto.request;

import com.datemate.domain.member.entity.enums.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 소셜 로그인 요청 DTO
 * 1. 클라이언트가 소셜 SDK에서 받은 토큰을 서버에 전달한다
 * 2. 서버는 이 토큰으로 소셜 플랫폼에 사용자 정보를 조회한다
 *
 * @param provider 인증 제공자 (KAKAO, APPLE)
 * @param socialToken 소셜 플랫폼에서 발급받은 액세스 토큰 또는 identity token
 * @param deviceInfo 로그인 기기 정보 (다중 기기 토큰 관리용)
 */
public record LoginRequest(
    @NotNull(message = "인증 제공자는 필수입니다.")
    AuthProvider provider,

    @NotBlank(message = "소셜 토큰은 필수입니다.")
    String socialToken,

    String deviceInfo
) {
}
