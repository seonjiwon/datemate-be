package com.datemate.domain.course.repository;

import com.datemate.domain.course.entity.CourseRequest;
import com.datemate.domain.course.entity.StationCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 중간역 후보 저장소
 * 1. 특정 코스 요청에 대한 중간역 후보 목록을 관리한다
 */
public interface StationCandidateRepository extends JpaRepository<StationCandidate, Long> {

    /**
     * 1. 특정 코스 요청의 중간역 후보를 소요시간 합 기준으로 정렬하여 조회한다
     * 2. 양측 소요시간 합이 작은 역이 가장 공평한 중간지점이다
     */
    List<StationCandidate> findByCourseRequestOrderByTravelTimeFromADesc(CourseRequest courseRequest);
}
