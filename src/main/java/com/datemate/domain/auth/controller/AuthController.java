package com.datemate.domain.auth.controller;

import com.datemate.domain.auth.dto.request.LoginRequest;
import com.datemate.domain.auth.dto.request.TokenRefreshRequest;
import com.datemate.domain.auth.dto.response.TokenResponse;
import com.datemate.domain.auth.service.AuthService;
import com.datemate.global.CustomResponse;
import com.datemate.global.code.success.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 * 1. 소셜 로그인, 토큰 갱신, 로그아웃 엔드포인트를 제공한다
 * 2. JWT 인증이 필요 없는 공개 API이다 (SecurityConfig에서 permitAll)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 1. 소셜 토큰으로 로그인/회원가입하고 JWT 쌍을 발급한다
     * 2. 신규 사용자면 자동 가입 후 토큰을 발급한다
     *
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<CustomResponse<TokenResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(CustomResponse.onSuccess(tokenResponse));
    }

    /**
     * 1. 리프레시 토큰으로 새 JWT 쌍을 발급한다
     * 2. token rotation 적용 — 리프레시 토큰도 함께 갱신된다
     *
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<CustomResponse<TokenResponse>> refreshToken(
        @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(CustomResponse.onSuccess(tokenResponse));
    }

    /**
     * 1. 로그아웃 처리 — 모든 기기의 리프레시 토큰을 삭제한다
     *
     * POST /api/v1/auth/logout
     *
     * TODO: @AuthenticationPrincipal Member member 주입 후 authService.logout(member) 호출
     */
    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<?>> logout() {
        // TODO: authService.logout(member);
        return ResponseEntity.ok(CustomResponse.onSuccess(GeneralSuccessCode.OK));
    }
}
