package com.datemate.domain.course.repository;

import com.datemate.domain.course.entity.Course;
import com.datemate.domain.member.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 코스 저장소
 * 1. 회원별 코스 목록 조회에 사용한다
 * 2. createdAt 역순 정렬로 최신 코스를 먼저 보여준다
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 1. 특정 회원의 코스 목록을 최신순으로 조회한다
     * 2. 마이페이지 "내 코스" 탭에서 사용한다
     */
    List<Course> findByMemberOrderByCreatedAtDesc(Member member);
}
