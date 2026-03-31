package com.datemate.domain.course.dto.request;

import com.datemate.domain.course.entity.enums.Mood;
import com.datemate.domain.course.entity.enums.Transport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 코스 생성 요청 DTO
 * 1. 프론트엔드 4단계 플로우의 최종 입력을 모아서 전달한다
 * 2. 출발지 A/B, 선택된 중간역, 예산, 분위기, 이동 수단을 포함한다
 *
 * @param originAAddress A 출발 주소
 * @param originALat A 출발지 위도
 * @param originALng A 출발지 경도
 * @param originBAddress B 출발 주소
 * @param originBLat B 출발지 위도
 * @param originBLng B 출발지 경도
 * @param selectedStationName 선택된 중간역 이름
 * @param selectedStationLat 선택된 중간역 위도
 * @param selectedStationLng 선택된 중간역 경도
 * @param budgetMin 최소 예산 (원)
 * @param budgetMax 최대 예산 (원)
 * @param mood 선호 분위기
 * @param transport 선호 이동 수단
 */
public record CourseCreateRequest(
    @NotBlank(message = "A 출발 주소는 필수입니다.")
    String originAAddress,

    @NotNull(message = "A 출발지 위도는 필수입니다.")
    BigDecimal originALat,

    @NotNull(message = "A 출발지 경도는 필수입니다.")
    BigDecimal originALng,

    @NotBlank(message = "B 출발 주소는 필수입니다.")
    String originBAddress,

    @NotNull(message = "B 출발지 위도는 필수입니다.")
    BigDecimal originBLat,

    @NotNull(message = "B 출발지 경도는 필수입니다.")
    BigDecimal originBLng,

    @NotBlank(message = "중간역 이름은 필수입니다.")
    String selectedStationName,

    @NotNull(message = "중간역 위도는 필수입니다.")
    BigDecimal selectedStationLat,

    @NotNull(message = "중간역 경도는 필수입니다.")
    BigDecimal selectedStationLng,

    @NotNull(message = "최소 예산은 필수입니다.")
    @Positive(message = "최소 예산은 양수여야 합니다.")
    Integer budgetMin,

    @NotNull(message = "최대 예산은 필수입니다.")
    @Positive(message = "최대 예산은 양수여야 합니다.")
    Integer budgetMax,

    @NotNull(message = "분위기 선택은 필수입니다.")
    Mood mood,

    @NotNull(message = "이동 수단 선택은 필수입니다.")
    Transport transport
) {
}
