package com.datemate.domain.member.dto.response;

import com.datemate.domain.member.entity.Member;
import com.datemate.domain.member.entity.enums.AuthProvider;

/**
 * 회원 프로필 응답 DTO
 * 1. 마이페이지 및 홈 화면에서 사용자 정보를 표시할 때 사용한다
 * 2. 엔티티를 직접 노출하지 않고 필요한 필드만 선별한다
 *
 * @param memberId 회원 고유 ID
 * @param nickname 표시 이름
 * @param profileImageUrl 프로필 이미지 URL
 * @param authProvider 로그인 수단 (KAKAO, APPLE)
 */
public record MemberProfileResponse(
    Long memberId,
    String nickname,
    String profileImageUrl,
    AuthProvider authProvider
) {

    /**
     * 1. Member 엔티티에서 응답 DTO로 변환한다
     * 2. 정적 팩토리 메서드로 변환 책임을 DTO에 둔다
     */
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
            member.getId(),
            member.getNickname(),
            member.getProfileImageUrl(),
            member.getAuthProvider()
        );
    }
}
