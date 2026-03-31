package com.datemate.domain.member.entity.enums;

/**
 * 사용자 권한 등급
 * 1. Spring Security 인가 처리에서 사용한다
 * 2. ROLE_ prefix는 SecurityConfig에서 자동 부여된다
 */
public enum UserRole {

    // 일반 사용자
    USER,

    // 관리자 (향후 어드민 패널용)
    ADMIN
}
