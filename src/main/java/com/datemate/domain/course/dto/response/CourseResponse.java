package com.datemate.domain.course.dto.response;

import com.datemate.domain.course.entity.Course;
import com.datemate.domain.course.entity.enums.CourseStatus;
import java.util.List;

/**
 * 코스 상세 응답 DTO
 * 1. 코스 생성 결과 및 상세 조회 시 반환한다
 * 2. 코스 기본 정보 + 장소 목록(타임라인)을 포함한다
 *
 * @param courseId 코스 고유 ID
 * @param title 코스 제목
 * @param description 코스 설명
 * @param totalDuration 총 소요 시간 (분)
 * @param totalCostMin 예상 최소 비용 (원)
 * @param totalCostMax 예상 최대 비용 (원)
 * @param startTime 코스 시작 시각 (HH:mm)
 * @param status 코스 상태
 * @param places 장소 타임라인 목록
 */
public record CourseResponse(
    Long courseId,
    String title,
    String description,
    Integer totalDuration,
    Integer totalCostMin,
    Integer totalCostMax,
    String startTime,
    CourseStatus status,
    List<CoursePlaceResponse> places
) {

    /**
     * 1. Course 엔티티와 장소 목록에서 응답 DTO를 생성한다
     * 2. places는 별도로 변환하여 주입한다 — 엔티티 연관관계 직접 접근을 피한다
     */
    public static CourseResponse of(Course course, List<CoursePlaceResponse> places) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getTotalDuration(),
            course.getTotalCostMin(),
            course.getTotalCostMax(),
            course.getStartTime(),
            course.getStatus(),
            places
        );
    }
}
