package com.datemate.domain.course.repository;

import com.datemate.domain.course.entity.Course;
import com.datemate.domain.course.entity.CourseShare;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 코스 공유 저장소
 * 1. 공유 토큰 기반 코스 조회에 사용한다
 * 2. 비회원도 토큰으로 코스를 열람할 수 있다
 */
public interface CourseShareRepository extends JpaRepository<CourseShare, Long> {

    /**
     * 1. 공유 토큰으로 공유 정보를 조회한다
     * 2. 딥링크 접속 시 토큰을 검증하고 코스를 반환하는 데 사용한다
     */
    Optional<CourseShare> findByShareToken(String shareToken);

    /**
     * 1. 특정 코스의 기존 공유 링크를 조회한다
     * 2. 이미 공유 링크가 있으면 새로 생성하지 않고 기존 것을 반환한다
     */
    Optional<CourseShare> findByCourse(Course course);
}
