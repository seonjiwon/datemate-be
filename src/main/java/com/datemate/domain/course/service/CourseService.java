package com.datemate.domain.course.service;

import com.datemate.domain.course.code.CourseErrorCode;
import com.datemate.domain.course.dto.request.CourseCreateRequest;
import com.datemate.domain.course.dto.response.CoursePlaceResponse;
import com.datemate.domain.course.dto.response.CourseResponse;
import com.datemate.domain.course.dto.response.CourseShareResponse;
import com.datemate.domain.course.entity.Course;
import com.datemate.domain.course.entity.CoursePlace;
import com.datemate.domain.course.entity.CourseRequest;
import com.datemate.domain.course.entity.CourseShare;
import com.datemate.domain.course.entity.enums.CourseRequestStatus;
import com.datemate.domain.course.entity.enums.CourseStatus;
import com.datemate.domain.course.entity.enums.TravelMethod;
import com.datemate.domain.course.repository.CoursePlaceRepository;
import com.datemate.domain.course.repository.CourseRepository;
import com.datemate.domain.course.repository.CourseRequestRepository;
import com.datemate.domain.course.repository.CourseShareRepository;
import com.datemate.domain.member.entity.Member;
import com.datemate.domain.place.entity.Place;
import com.datemate.domain.place.entity.enums.PlaceCategory;
import com.datemate.domain.place.repository.PlaceRepository;
import com.datemate.domain.place.service.GooglePlacesService;
import com.datemate.global.exception.CustomException;
import com.datemate.infra.ai.GeminiClient;
import com.datemate.infra.ai.dto.GeminiResponse;
import com.datemate.infra.ai.dto.GeminiToolCall;
import com.datemate.infra.ai.prompt.CoursePromptConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 생성 서비스 (오케스트레이터)
 * 1. Gemini 에이전트 루프를 구동하여 AI 기반 코스를 생성한다
 * 2. GeminiClient와 GooglePlacesService를 조합하는 오케스트레이션만 담당한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final int MAX_AGENT_LOOPS = 5;

    private final GeminiClient geminiClient;
    private final GooglePlacesService googlePlacesService;
    private final CourseRequestRepository courseRequestRepository;
    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseShareRepository courseShareRepository;
    private final PlaceRepository placeRepository;

    /**
     * AI 데이트 코스 생성
     */
    @Transactional
    public CourseResponse createCourse(Member member, CourseCreateRequest request) {
        log.info("[CourseService] 코스 생성 시작 — memberId={}, station={}, mood={}, budget={}-{}",
            member.getId(), request.selectedStationName(), request.mood(), request.budgetMin(), request.budgetMax());

        // 1. 코스 요청을 저장한다
        CourseRequest courseRequest = saveCourseRequest(member, request);
        log.debug("[CourseService] CourseRequest 저장 완료 — courseRequestId={}", courseRequest.getId());

        try {
            // 2. Gemini 에이전트 루프를 실행하여 코스 JSON을 생성한다
            courseRequest.updateStatus(CourseRequestStatus.PROCESSING);
            log.info("[CourseService] 에이전트 루프 실행 시작");

            long agentStart = System.currentTimeMillis();
            String courseJson = runAgentLoop(request);
            long agentElapsed = System.currentTimeMillis() - agentStart;
            log.info("[CourseService] 에이전트 루프 완료 — 소요시간: {}ms, 응답 길이: {}자",
                agentElapsed, courseJson.length());
            log.debug("[CourseService] AI 응답 JSON (앞 500자): {}", courseJson.substring(0, Math.min(500, courseJson.length())));

            // 3. AI 응답을 파싱하여 Course + CoursePlace를 저장한다
            Course course = parseCourseAndSave(courseJson, courseRequest, member);
            log.info("[CourseService] Course 저장 완료 — courseId={}, title={}",
                course.getId(), course.getTitle());

            // 4. 요청 상태를 완료로 변경한다
            courseRequest.updateStatus(CourseRequestStatus.COMPLETED);

            // 5. 응답 DTO를 조립하여 반환한다
            CourseResponse response = buildCourseResponse(course);
            log.info("[CourseService] 코스 생성 전체 완료 — courseId={}", course.getId());

            return response;

        } catch (CustomException e) {
            courseRequest.updateStatus(CourseRequestStatus.FAILED);
            log.error("[CourseService] 코스 생성 실패 (CustomException) — code={}, message={}",
                e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            courseRequest.updateStatus(CourseRequestStatus.FAILED);
            log.error("[CourseService] 코스 생성 실패 (예외) — {}", e.getMessage(), e);
            throw new CustomException(CourseErrorCode.COURSE_GENERATION_FAILED);
        }
    }

    /**
     * 코스 상세 조회
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(Long courseId, Member member) {
        log.debug("[CourseService] 코스 상세 조회 — courseId={}, memberId={}", courseId, member.getId());

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> {
                log.warn("[CourseService] 코스 미발견 — courseId={}", courseId);
                return new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
            });

        if (!course.getMember().getId().equals(member.getId())) {
            log.warn("[CourseService] 코스 접근 거부 — courseId={}, ownerId={}, requesterId={}",
                courseId, course.getMember().getId(), member.getId());
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        return buildCourseResponse(course);
    }

    /**
     * 코스 확정
     */
    @Transactional
    public void confirmCourse(Long courseId, Member member) {
        log.info("[CourseService] 코스 확정 — courseId={}, memberId={}", courseId, member.getId());

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> {
                log.warn("[CourseService] 확정 대상 코스 미발견 — courseId={}", courseId);
                return new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
            });

        if (!course.getMember().getId().equals(member.getId())) {
            log.warn("[CourseService] 코스 확정 권한 없음 — courseId={}, ownerId={}, requesterId={}",
                courseId, course.getMember().getId(), member.getId());
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        if (course.getStatus() != CourseStatus.DRAFT) {
            log.warn("[CourseService] 코스 이미 확정됨 — courseId={}, status={}", courseId, course.getStatus());
            throw new CustomException(CourseErrorCode.COURSE_ALREADY_CONFIRMED);
        }

        course.confirm();
        log.info("[CourseService] 코스 확정 완료 — courseId={}", courseId);
    }

    /**
     * 코스 공유 링크 생성
     */
    @Transactional
    public CourseShareResponse shareCourse(Long courseId, Member member) {
        log.info("[CourseService] 공유 링크 생성 — courseId={}, memberId={}", courseId, member.getId());

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (!course.getMember().getId().equals(member.getId())) {
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        CourseShare share = courseShareRepository.findByCourse(course)
            .orElseGet(() -> {
                log.debug("[CourseService] 신규 공유 토큰 생성 — courseId={}", courseId);
                CourseShare newShare = CourseShare.builder()
                    .course(course)
                    .shareToken(UUID.randomUUID().toString().replace("-", ""))
                    .expiresAt(LocalDateTime.now().plusHours(72))
                    .build();
                return courseShareRepository.save(newShare);
            });

        log.info("[CourseService] 공유 링크 생성 완료 — courseId={}, token={}", courseId, share.getShareToken());

        return CourseShareResponse.of(share, "datemate://");
    }

    /**
     * 공유 토큰으로 코스 조회 (비회원 접근 가능)
     */
    @Transactional
    public CourseResponse getCourseByShareToken(String shareToken) {
        log.info("[CourseService] 공유 토큰으로 코스 조회 — token={}", shareToken);

        CourseShare share = courseShareRepository.findByShareToken(shareToken)
            .orElseThrow(() -> {
                log.warn("[CourseService] 공유 토큰 미발견 — token={}", shareToken);
                return new CustomException(CourseErrorCode.SHARE_TOKEN_NOT_FOUND);
            });

        if (share.isExpired()) {
            log.warn("[CourseService] 공유 토큰 만료 — token={}, expiresAt={}", shareToken, share.getExpiresAt());
            throw new CustomException(CourseErrorCode.SHARE_TOKEN_EXPIRED);
        }

        share.incrementViewCount();
        log.debug("[CourseService] 공유 조회수 증가 — token={}, viewCount={}", shareToken, share.getViewCount());

        return buildCourseResponse(share.getCourse());
    }

    /**
     * 내 코스 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getMyCourses(Member member) {
        log.debug("[CourseService] 내 코스 목록 조회 — memberId={}", member.getId());

        List<CourseResponse> result = courseRepository.findByMemberOrderByCreatedAtDesc(member)
                               .stream()
                               .map(this::buildCourseResponse)
                               .toList();

        log.debug("[CourseService] 내 코스 목록 조회 완료 — memberId={}, count={}", member.getId(), result.size());
        return result;
    }

    // ============================================================
    // Private: 에이전트 루프
    // ============================================================

    private String runAgentLoop(CourseCreateRequest request) {
        List<String> conversationHistory = new ArrayList<>();

        String userMessage = String.format(
            CoursePromptConstants.COURSE_USER_MESSAGE_TEMPLATE,
            request.selectedStationName(),
            request.mood().name(),
            request.budgetMin(),
            request.budgetMax(),
            request.transport().name()
        );
        conversationHistory.add(geminiClient.buildUserMessage(userMessage));
        log.debug("[CourseService] 사용자 메시지 생성 완료 — 길이: {}자", userMessage.length());

        for (int loop = 0; loop < MAX_AGENT_LOOPS; loop++) {
            log.info("[CourseService] === 에이전트 루프 {}회차 시작 ===", loop + 1);

            long loopStart = System.currentTimeMillis();
            GeminiResponse response = geminiClient.chat(
                CoursePromptConstants.COURSE_SYSTEM_INSTRUCTION,
                conversationHistory
            );
            long loopElapsed = System.currentTimeMillis() - loopStart;
            log.info("[CourseService] Gemini API 응답 수신 — {}회차, 소요시간: {}ms", loop + 1, loopElapsed);

            conversationHistory.add(response.rawModelContent());

            // 텍스트 응답이면 루프 종료
            if (response.isTextResponse()) {
                log.info("[CourseService] AI 최종 텍스트 응답 수신 — 루프 종료 (총 {}회차)", loop + 1);
                return response.textContent().orElse("");
            }

            // 도구 호출이면 실행하고 결과를 피드백
            if (response.isToolCallResponse()) {
                GeminiToolCall toolCall = response.toolCall().orElseThrow();
                log.info("[CourseService] AI 도구 호출 요청: function={}, args={}",
                    toolCall.functionName(), toolCall.arguments());

                long toolStart = System.currentTimeMillis();
                String toolResult = executeToolCall(toolCall);
                long toolElapsed = System.currentTimeMillis() - toolStart;
                log.info("[CourseService] 도구 실행 완료 — function={}, 소요시간: {}ms, 결과 길이: {}자",
                    toolCall.functionName(), toolElapsed, toolResult.length());
                log.debug("[CourseService] 도구 결과 (앞 300자): {}",
                    toolResult.substring(0, Math.min(300, toolResult.length())));

                String functionResponseMsg = geminiClient.buildFunctionResponseMessage(
                    toolCall.functionName(), toolResult
                );
                conversationHistory.add(functionResponseMsg);
            }
        }

        log.error("[CourseService] 에이전트 루프 최대 횟수({}) 초과", MAX_AGENT_LOOPS);
        throw new CustomException(CourseErrorCode.COURSE_GENERATION_LOOP_EXCEEDED);
    }

    private String executeToolCall(GeminiToolCall toolCall) {
        if (!"search_nearby_places".equals(toolCall.functionName())) {
            log.warn("[CourseService] 알 수 없는 도구 호출: {}", toolCall.functionName());
            return "{\"error\": \"unknown function\"}";
        }

        JsonObject args = toolCall.arguments();
        double lat = args.has("lat") ? args.get("lat").getAsDouble() : 37.5665;
        double lng = args.has("lng") ? args.get("lng").getAsDouble() : 126.9780;
        String category = args.has("category") ? args.get("category").getAsString() : "restaurant";
        double radius = args.has("radius") ? args.get("radius").getAsDouble() : 1000.0;

        log.info("[CourseService] search_nearby_places 실행 — lat={}, lng={}, category={}, radius={}",
            lat, lng, category, radius);

        return googlePlacesService.searchNearbyPlaces(lat, lng, category, radius);
    }

    // ============================================================
    // Private: 엔티티 저장 / 변환
    // ============================================================

    private CourseRequest saveCourseRequest(Member member, CourseCreateRequest request) {
        CourseRequest courseRequest = CourseRequest.builder()
            .member(member)
            .originAAddress(request.originAAddress())
            .originALat(request.originALat())
            .originALng(request.originALng())
            .originBAddress(request.originBAddress())
            .originBLat(request.originBLat())
            .originBLng(request.originBLng())
            .selectedStationName(request.selectedStationName())
            .selectedStationLat(request.selectedStationLat())
            .selectedStationLng(request.selectedStationLng())
            .budgetMin(request.budgetMin())
            .budgetMax(request.budgetMax())
            .mood(request.mood())
            .transport(request.transport())
            .build();

        CourseRequest saved = courseRequestRepository.save(courseRequest);
        log.debug("[CourseService] CourseRequest 저장 — id={}, station={}", saved.getId(), request.selectedStationName());
        return saved;
    }

    private Course parseCourseAndSave(String courseJson, CourseRequest courseRequest, Member member) {
        try {
            String cleanJson = courseJson.replaceAll("```json|```", "").trim();
            JsonObject parsed = JsonParser.parseString(cleanJson).getAsJsonObject();

            JsonArray placesArray = parsed.getAsJsonArray("places");
            log.debug("[CourseService] AI 응답 파싱 — 장소 {}개 발견", placesArray.size());

            int totalDuration = 0;
            int totalCostMin = 0;
            int totalCostMax = 0;

            for (JsonElement el : placesArray) {
                JsonObject p = el.getAsJsonObject();
                totalDuration += p.has("duration_minutes") ? p.get("duration_minutes").getAsInt() : 0;
                totalDuration += p.has("travel_time_to_next") ? p.get("travel_time_to_next").getAsInt() : 0;
                totalCostMin += p.has("cost_min") ? p.get("cost_min").getAsInt() : 0;
                totalCostMax += p.has("cost_max") ? p.get("cost_max").getAsInt() : 0;
            }

            String title = parsed.has("title") ? parsed.get("title").getAsString() : "AI 추천 코스";
            log.debug("[CourseService] 파싱 결과 — title={}, totalDuration={}분, costRange={}-{}원",
                title, totalDuration, totalCostMin, totalCostMax);

            Course course = Course.builder()
                .courseRequest(courseRequest)
                .member(member)
                .title(title)
                .description(parsed.has("description") ? parsed.get("description").getAsString() : null)
                .totalDuration(totalDuration)
                .totalCostMin(totalCostMin)
                .totalCostMax(totalCostMax)
                .build();

            Course savedCourse = courseRepository.save(course);
            log.info("[CourseService] Course 엔티티 저장 — courseId={}", savedCourse.getId());

            for (int i = 0; i < placesArray.size(); i++) {
                JsonObject p = placesArray.get(i).getAsJsonObject();
                String placeName = p.has("name") ? p.get("name").getAsString() : "이름 없음";
                log.debug("[CourseService] 장소 저장 중 — [{}/{}] {}", i + 1, placesArray.size(), placeName);
                saveCoursePlaceFromJson(p, savedCourse);
            }

            log.info("[CourseService] CoursePlace 전체 저장 완료 — courseId={}, placeCount={}",
                savedCourse.getId(), placesArray.size());

            return savedCourse;

        } catch (Exception e) {
            log.error("[CourseService] AI 응답 파싱 실패 — JSON 앞 500자: {}",
                courseJson.substring(0, Math.min(500, courseJson.length())), e);
            throw new CustomException(CourseErrorCode.COURSE_GENERATION_FAILED);
        }
    }

    private void saveCoursePlaceFromJson(JsonObject placeJson, Course course) {
        String googlePlaceId = placeJson.has("google_place_id")
            ? placeJson.get("google_place_id").getAsString() : null;

        Place place;
        if (googlePlaceId != null) {
            place = placeRepository.findByGooglePlaceId(googlePlaceId)
                .orElseGet(() -> {
                    log.debug("[CourseService] 신규 Place 생성 — googlePlaceId={}", googlePlaceId);
                    return placeRepository.save(
                        Place.builder()
                            .googlePlaceId(googlePlaceId)
                            .name(placeJson.has("name") ? placeJson.get("name").getAsString() : "이름 없음")
                            .address("")
                            .latitude(java.math.BigDecimal.ZERO)
                            .longitude(java.math.BigDecimal.ZERO)
                            .category(parsePlaceCategory(placeJson))
                            .build()
                    );
                });
        } else {
            String generatedId = "generated-" + UUID.randomUUID();
            log.debug("[CourseService] google_place_id 없음 — 생성된 ID={}", generatedId);
            place = placeRepository.save(
                Place.builder()
                    .googlePlaceId(generatedId)
                    .name(placeJson.has("name") ? placeJson.get("name").getAsString() : "이름 없음")
                    .address("")
                    .latitude(java.math.BigDecimal.ZERO)
                    .longitude(java.math.BigDecimal.ZERO)
                    .category(parsePlaceCategory(placeJson))
                    .build()
            );
        }

        CoursePlace coursePlace = CoursePlace.builder()
            .course(course)
            .place(place)
            .orderIndex(placeJson.has("order") ? placeJson.get("order").getAsInt() - 1 : 0)
            .durationMinutes(placeJson.has("duration_minutes") ? placeJson.get("duration_minutes").getAsInt() : 60)
            .costMin(placeJson.has("cost_min") ? placeJson.get("cost_min").getAsInt() : null)
            .costMax(placeJson.has("cost_max") ? placeJson.get("cost_max").getAsInt() : null)
            .travelMethodToNext(parseTravelMethod(placeJson))
            .travelTimeToNext(placeJson.has("travel_time_to_next") ? placeJson.get("travel_time_to_next").getAsInt() : null)
            .memo(placeJson.has("memo") ? placeJson.get("memo").getAsString() : null)
            .build();

        coursePlaceRepository.save(coursePlace);
    }

    private PlaceCategory parsePlaceCategory(JsonObject placeJson) {
        if (!placeJson.has("category")) {
            return PlaceCategory.ETC;
        }
        try {
            return PlaceCategory.valueOf(placeJson.get("category").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("[CourseService] PlaceCategory 매칭 실패 — value={}, fallback=ETC",
                placeJson.get("category").getAsString());
            return PlaceCategory.ETC;
        }
    }

    private TravelMethod parseTravelMethod(JsonObject placeJson) {
        if (!placeJson.has("travel_method_to_next")) {
            return TravelMethod.NONE;
        }
        try {
            return TravelMethod.valueOf(placeJson.get("travel_method_to_next").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("[CourseService] TravelMethod 매칭 실패 — value={}, fallback=NONE",
                placeJson.get("travel_method_to_next").getAsString());
            return TravelMethod.NONE;
        }
    }

    private CourseResponse buildCourseResponse(Course course) {
        List<CoursePlace> coursePlaces = coursePlaceRepository.findByCourseOrderByOrderIndexAsc(course);

        List<CoursePlaceResponse> placeResponses = coursePlaces.stream()
            .map(cp -> CoursePlaceResponse.from(cp, cp.getPlace()))
            .toList();

        log.debug("[CourseService] CourseResponse 조립 — courseId={}, placeCount={}", course.getId(), placeResponses.size());

        return CourseResponse.of(course, placeResponses);
    }
}
