package com.datemate.domain.course.controller;

import com.datemate.domain.course.dto.request.CourseCreateRequest;
import com.datemate.domain.course.dto.response.CourseResponse;
import com.datemate.domain.course.dto.response.CourseShareResponse;
import com.datemate.domain.course.service.CourseService;
import com.datemate.domain.member.entity.Member;
import com.datemate.domain.member.repository.MemberRepository;
import com.datemate.global.CustomResponse;
import com.datemate.global.code.success.GeneralSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코스 API 컨트롤러
 * 1. 코스 생성, 조회, 확정, 공유 엔드포인트를 제공한다
 * 2. 인증 필요 API — JWT Bearer 토큰 필수 (공유 링크 조회 제외)
 *
 * TODO: 프로덕션에서는 @AuthenticationPrincipal로 Member를 주입받도록 전환할 것
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final MemberRepository memberRepository;

    // ── Dev용 임시 멤버 조회 ──
    // TODO: @AuthenticationPrincipal 구현 후 제거
    private Member getDevMember() {
        return memberRepository.findById(1L)
            .orElseThrow(() -> new RuntimeException(
                "[Dev] member id=1이 없습니다. DB에 테스트 유저를 먼저 넣어주세요."
            ));
    }

    /**
     * 1. AI 기반 데이트 코스를 생성한다
     * 2. Gemini 에이전트 루프를 실행하므로 응답 시간이 길 수 있다 (10~30초)
     *
     * POST /api/v1/courses
     */
    @PostMapping
    public ResponseEntity<CustomResponse<CourseResponse>> createCourse(
        // TODO: @AuthenticationPrincipal Member member,
        @Valid @RequestBody CourseCreateRequest request
    ) {
        // 1. dev용 임시 멤버로 코스 생성
        Member member = getDevMember();
        CourseResponse response = courseService.createCourse(member, request);

        // 2. 201 CREATED로 응답
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CustomResponse.onSuccess(GeneralSuccessCode.CREATED, response));
    }

    /**
     * 1. 코스 상세를 조회한다
     * 2. 코스 소유자만 접근 가능하다
     *
     * GET /api/v1/courses/{courseId}
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CustomResponse<CourseResponse>> getCourseDetail(
        // TODO: @AuthenticationPrincipal Member member,
        @PathVariable Long courseId
    ) {
        Member member = getDevMember();
        CourseResponse response = courseService.getCourseDetail(courseId, member);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 1. 코스를 확정(CONFIRMED) 상태로 변경한다
     * 2. DRAFT 상태에서만 가능하다
     *
     * POST /api/v1/courses/{courseId}/confirm
     */
    @PostMapping("/{courseId}/confirm")
    public ResponseEntity<CustomResponse<?>> confirmCourse(
        // TODO: @AuthenticationPrincipal Member member,
        @PathVariable Long courseId
    ) {
        Member member = getDevMember();
        courseService.confirmCourse(courseId, member);
        return ResponseEntity.ok(CustomResponse.onSuccess(GeneralSuccessCode.OK));
    }

    /**
     * 1. 코스 공유 링크를 생성한다
     * 2. 이미 공유 링크가 있으면 기존 것을 반환한다
     *
     * POST /api/v1/courses/{courseId}/share
     */
    @PostMapping("/{courseId}/share")
    public ResponseEntity<CustomResponse<CourseShareResponse>> shareCourse(
        // TODO: @AuthenticationPrincipal Member member,
        @PathVariable Long courseId
    ) {
        Member member = getDevMember();
        CourseShareResponse response = courseService.shareCourse(courseId, member);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 1. 공유 토큰으로 코스를 조회한다 (비회원 접근 가능)
     * 2. SecurityConfig에서 이 경로를 permitAll로 설정해야 한다
     *
     * GET /api/v1/courses/shared?token={shareToken}
     */
    @GetMapping("/shared")
    public ResponseEntity<CustomResponse<CourseResponse>> getCourseByShareToken(
        @RequestParam String token
    ) {
        CourseResponse response = courseService.getCourseByShareToken(token);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 1. 내 코스 목록을 조회한다
     * 2. 최신순으로 정렬하여 반환한다
     *
     * GET /api/v1/courses/my
     */
    @GetMapping("/my")
    public ResponseEntity<CustomResponse<List<CourseResponse>>> getMyCourses(
        // TODO: @AuthenticationPrincipal Member member
    ) {
        Member member = getDevMember();
        List<CourseResponse> responses = courseService.getMyCourses(member);
        return ResponseEntity.ok(CustomResponse.onSuccess(responses));
    }
}
