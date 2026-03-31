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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final MemberRepository memberRepository;

    // ── Dev용 임시 멤버 조회 ──
    private Member getDevMember() {
        log.debug("[CourseController] Dev 멤버 조회 시도 (id=1)");
        return memberRepository.findById(1L)
            .orElseThrow(() -> {
                log.error("[CourseController] Dev 멤버(id=1)가 DB에 존재하지 않습니다. INSERT INTO member 필요");
                return new RuntimeException(
                    "[Dev] member id=1이 없습니다. DB에 테스트 유저를 먼저 넣어주세요."
                );
            });
    }

    /**
     * POST /api/v1/courses — AI 기반 데이트 코스 생성
     */
    @PostMapping
    public ResponseEntity<CustomResponse<CourseResponse>> createCourse(
        @Valid @RequestBody CourseCreateRequest request
    ) {
        log.info("[CourseController] POST /courses — 코스 생성 요청 수신: station={}, mood={}, budget={}-{}, transport={}",
            request.selectedStationName(), request.mood(), request.budgetMin(), request.budgetMax(), request.transport());

        Member member = getDevMember();
        long startTime = System.currentTimeMillis();

        CourseResponse response = courseService.createCourse(member, request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[CourseController] POST /courses — 코스 생성 완료 (소요시간: {}ms)", elapsed);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CustomResponse.onSuccess(GeneralSuccessCode.CREATED, response));
    }

    /**
     * GET /api/v1/courses/{courseId} — 코스 상세 조회
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CustomResponse<CourseResponse>> getCourseDetail(
        @PathVariable Long courseId
    ) {
        log.info("[CourseController] GET /courses/{} — 코스 상세 조회 요청", courseId);

        Member member = getDevMember();
        CourseResponse response = courseService.getCourseDetail(courseId, member);

        log.debug("[CourseController] GET /courses/{} — 조회 완료", courseId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * POST /api/v1/courses/{courseId}/confirm — 코스 확정
     */
    @PostMapping("/{courseId}/confirm")
    public ResponseEntity<CustomResponse<?>> confirmCourse(
        @PathVariable Long courseId
    ) {
        log.info("[CourseController] POST /courses/{}/confirm — 코스 확정 요청", courseId);

        Member member = getDevMember();
        courseService.confirmCourse(courseId, member);

        log.info("[CourseController] POST /courses/{}/confirm — 확정 완료", courseId);

        return ResponseEntity.ok(CustomResponse.onSuccess(GeneralSuccessCode.OK));
    }

    /**
     * POST /api/v1/courses/{courseId}/share — 공유 링크 생성
     */
    @PostMapping("/{courseId}/share")
    public ResponseEntity<CustomResponse<CourseShareResponse>> shareCourse(
        @PathVariable Long courseId
    ) {
        log.info("[CourseController] POST /courses/{}/share — 공유 링크 생성 요청", courseId);

        Member member = getDevMember();
        CourseShareResponse response = courseService.shareCourse(courseId, member);

        log.info("[CourseController] POST /courses/{}/share — 공유 토큰 생성: token={}",
            courseId, response.shareToken());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * GET /api/v1/courses/shared?token={shareToken} — 공유 토큰으로 코스 조회
     */
    @GetMapping("/shared")
    public ResponseEntity<CustomResponse<CourseResponse>> getCourseByShareToken(
        @RequestParam String token
    ) {
        log.info("[CourseController] GET /courses/shared — 공유 토큰 조회: token={}", token);

        CourseResponse response = courseService.getCourseByShareToken(token);

        log.debug("[CourseController] GET /courses/shared — 조회 완료");

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * GET /api/v1/courses/my — 내 코스 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<CustomResponse<List<CourseResponse>>> getMyCourses() {
        log.info("[CourseController] GET /courses/my — 내 코스 목록 조회");

        Member member = getDevMember();
        List<CourseResponse> responses = courseService.getMyCourses(member);

        log.info("[CourseController] GET /courses/my — {}건 반환", responses.size());

        return ResponseEntity.ok(CustomResponse.onSuccess(responses));
    }
}
