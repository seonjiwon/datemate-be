package com.datemate.domain.course.repository;

import com.datemate.domain.course.entity.Course;
import com.datemate.domain.course.entity.CoursePlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 코스-장소 연결 저장소
 * 1. 코스에 포함된 장소 목록을 순서대로 조회한다
 */
public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    /**
     * 1. 특정 코스의 장소를 방문 순서(order_index)대로 조회한다
     * 2. 타임라인 렌더링에 사용한다
     */
    List<CoursePlace> findByCourseOrderByOrderIndexAsc(Course course);
}
