package com.datemate.domain.member.repository;

import com.datemate.domain.member.entity.Member;
import com.datemate.domain.member.entity.enums.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 저장소
 * 1. 소셜 로그인 시 기존 회원 조회에 사용한다
 * 2. socialId + authProvider 조합이 사실상 유니크 키 역할을 한다
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 1. 소셜 플랫폼 ID와 인증 제공자로 회원을 조회한다
     * 2. 카카오 사용자와 Apple 사용자를 분리하여 검색한다
     */
    Optional<Member> findBySocialIdAndAuthProvider(String socialId, AuthProvider authProvider);

    /**
     * 1. 해당 소셜 계정으로 가입된 회원이 존재하는지 확인한다
     * 2. 신규 가입 vs 기존 로그인 분기 처리에 사용한다
     */
    boolean existsBySocialIdAndAuthProvider(String socialId, AuthProvider authProvider);
}
