package com.datemate.domain.auth.controller;

import com.datemate.domain.auth.dto.request.LoginRequest;
import com.datemate.domain.auth.dto.request.TokenRefreshRequest;
import com.datemate.domain.auth.dto.response.TokenResponse;
import com.datemate.domain.auth.service.AuthService;
import com.datemate.global.CustomResponse;
import com.datemate.global.code.success.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/login — 소셜 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<CustomResponse<TokenResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        log.info("[AuthController] POST /auth/login — 로그인 요청: provider={}", request.provider());

        TokenResponse tokenResponse = authService.login(request);

        log.info("[AuthController] POST /auth/login — 로그인 성공: provider={}", request.provider());

        return ResponseEntity.ok(CustomResponse.onSuccess(tokenResponse));
    }

    /**
     * POST /api/v1/auth/refresh — 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<CustomResponse<TokenResponse>> refreshToken(
        @Valid @RequestBody TokenRefreshRequest request
    ) {
        log.info("[AuthController] POST /auth/refresh — 토큰 갱신 요청");

        TokenResponse tokenResponse = authService.refreshToken(request);

        log.debug("[AuthController] POST /auth/refresh — 갱신 완료");

        return ResponseEntity.ok(CustomResponse.onSuccess(tokenResponse));
    }

    /**
     * POST /api/v1/auth/logout — 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<?>> logout() {
        log.info("[AuthController] POST /auth/logout — 로그아웃 요청");

        // TODO: @AuthenticationPrincipal Member member 주입 후 authService.logout(member) 호출
        log.warn("[AuthController] POST /auth/logout — stub 상태 (미구현)");

        return ResponseEntity.ok(CustomResponse.onSuccess(GeneralSuccessCode.OK));
    }
}
