package com.datemate.domain.member.entity.enums;

/**
 * 소셜 로그인 제공자
 * 1. 현재 MVP에서 지원하는 인증 수단을 정의한다
 * 2. Apple은 App Store 심사 가이드라인 4.8 필수 요건이다
 */
public enum AuthProvider {

    // 카카오 소셜 로그인
    KAKAO,

    // Apple 소셜 로그인 (App Store 필수)
    APPLE
}
