package com.datemate.domain.course.repository;

import com.datemate.domain.course.entity.CourseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 코스 생성 요청 저장소
 * 1. 코스 생성 파이프라인의 입력 데이터를 영속화한다
 * 2. 요청 이력 조회 및 재생성에 사용한다
 */
public interface CourseRequestRepository extends JpaRepository<CourseRequest, Long> {
}
