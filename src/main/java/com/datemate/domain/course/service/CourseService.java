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
 * 3. 외부 API 호출은 모두 위임 — 이 클래스는 흐름 제어에 집중한다
 * 4. 테스트 시 GeminiClient, GooglePlacesService를 모킹하면 독립 테스트 가능하다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    // 에이전트 루프 최대 반복 횟수 — 무한 루프 방지
    private static final int MAX_AGENT_LOOPS = 5;

    private final GeminiClient geminiClient;
    private final GooglePlacesService googlePlacesService;
    private final CourseRequestRepository courseRequestRepository;
    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseShareRepository courseShareRepository;
    private final PlaceRepository placeRepository;

    /**
     * 1. 사용자 조건으로 AI 데이트 코스를 생성한다
     * 2. CourseRequest 저장 → Gemini 에이전트 루프 → Course + CoursePlace 저장
     * 3. 전체 과정이 하나의 트랜잭션으로 관리된다
     *
     * @param member 로그인한 회원
     * @param request 코스 생성 조건 (출발지, 중간역, 예산, 분위기 등)
     * @return 생성된 코스 상세 응답
     */
    @Transactional
    public CourseResponse createCourse(Member member, CourseCreateRequest request) {
        // 1. 코스 요청을 저장한다
        CourseRequest courseRequest = saveCourseRequest(member, request);

        try {
            // 2. Gemini 에이전트 루프를 실행하여 코스 JSON을 생성한다
            courseRequest.updateStatus(CourseRequestStatus.PROCESSING);
            String courseJson = runAgentLoop(request);

            // 3. AI 응답을 파싱하여 Course + CoursePlace를 저장한다
            Course course = parseCourseAndSave(courseJson, courseRequest, member);

            // 4. 요청 상태를 완료로 변경한다
            courseRequest.updateStatus(CourseRequestStatus.COMPLETED);

            // 5. 응답 DTO를 조립하여 반환한다
            return buildCourseResponse(course);

        } catch (CustomException e) {
            courseRequest.updateStatus(CourseRequestStatus.FAILED);
            throw e;
        } catch (Exception e) {
            courseRequest.updateStatus(CourseRequestStatus.FAILED);
            log.error("[CourseService] 코스 생성 실패", e);
            throw new CustomException(CourseErrorCode.COURSE_GENERATION_FAILED);
        }
    }

    /**
     * 1. 코스 상세를 조회한다
     * 2. 소유자 확인 후 CoursePlaceResponse 목록과 함께 반환한다
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(Long courseId, Member member) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (!course.getMember().getId().equals(member.getId())) {
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        return buildCourseResponse(course);
    }

    /**
     * 1. 코스를 확정(CONFIRMED) 상태로 변경한다
     * 2. DRAFT 상태에서만 확정 가능하다
     */
    @Transactional
    public void confirmCourse(Long courseId, Member member) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (!course.getMember().getId().equals(member.getId())) {
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new CustomException(CourseErrorCode.COURSE_ALREADY_CONFIRMED);
        }

        course.confirm();
    }

    /**
     * 1. 코스 공유 링크를 생성한다
     * 2. 이미 공유 링크가 있으면 기존 것을 반환한다
     * 3. 새 링크는 72시간 유효하다
     */
    @Transactional
    public CourseShareResponse shareCourse(Long courseId, Member member) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (!course.getMember().getId().equals(member.getId())) {
            throw new CustomException(CourseErrorCode.COURSE_ACCESS_DENIED);
        }

        CourseShare share = courseShareRepository.findByCourse(course)
            .orElseGet(() -> {
                CourseShare newShare = CourseShare.builder()
                    .course(course)
                    .shareToken(UUID.randomUUID().toString().replace("-", ""))
                    .expiresAt(LocalDateTime.now().plusHours(72))
                    .build();
                return courseShareRepository.save(newShare);
            });

        return CourseShareResponse.of(share, "datemate://");
    }

    /**
     * 1. 공유 토큰으로 코스를 조회한다 (비회원도 접근 가능)
     * 2. 만료 여부와 유효성을 검증한다
     */
    @Transactional
    public CourseResponse getCourseByShareToken(String shareToken) {
        CourseShare share = courseShareRepository.findByShareToken(shareToken)
            .orElseThrow(() -> new CustomException(CourseErrorCode.SHARE_TOKEN_NOT_FOUND));

        if (share.isExpired()) {
            throw new CustomException(CourseErrorCode.SHARE_TOKEN_EXPIRED);
        }

        share.incrementViewCount();
        return buildCourseResponse(share.getCourse());
    }

    /**
     * 1. 회원의 코스 목록을 조회한다
     * 2. 마이페이지에서 사용한다
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getMyCourses(Member member) {
        return courseRepository.findByMemberOrderByCreatedAtDesc(member)
                               .stream()
                               .map(this::buildCourseResponse)
                               .toList();
    }

    // ============================================================
    // Private: 에이전트 루프
    // ============================================================

    /**
     * 1. Gemini 에이전트 루프를 실행한다
     * 2. AI가 function call을 요청하면 GooglePlacesService로 실행 → 결과를 피드백 → 반복
     * 3. AI가 텍스트(최종 JSON)를 반환하면 루프를 종료한다
     * 4. 최대 MAX_AGENT_LOOPS회까지 반복하며, 초과 시 예외를 던진다
     */
    private String runAgentLoop(CourseCreateRequest request) {
        // 1. 대화 이력을 초기화한다
        List<String> conversationHistory = new ArrayList<>();

        // 2. 사용자 메시지를 생성하여 이력에 추가한다
        String userMessage = String.format(
            CoursePromptConstants.COURSE_USER_MESSAGE_TEMPLATE,
            request.selectedStationName(),
            request.mood().name(),
            request.budgetMin(),
            request.budgetMax(),
            request.transport().name()
        );
        conversationHistory.add(geminiClient.buildUserMessage(userMessage));

        // 3. 에이전트 루프를 실행한다
        for (int loop = 0; loop < MAX_AGENT_LOOPS; loop++) {
            log.info("[CourseService] 에이전트 루프 {}회차 실행", loop + 1);

            GeminiResponse response = geminiClient.chat(
                CoursePromptConstants.COURSE_SYSTEM_INSTRUCTION,
                conversationHistory
            );

            // 4. AI 응답을 대화 이력에 추가한다
            conversationHistory.add(response.rawModelContent());

            // 5. 텍스트 응답이면 루프를 종료한다
            if (response.isTextResponse()) {
                log.info("[CourseService] AI 최종 응답 수신 — 루프 종료");
                return response.textContent().orElse("");
            }

            // 6. 도구 호출이면 실행하고 결과를 피드백한다
            if (response.isToolCallResponse()) {
                GeminiToolCall toolCall = response.toolCall().orElseThrow();
                String toolResult = executeToolCall(toolCall);

                String functionResponseMsg = geminiClient.buildFunctionResponseMessage(
                    toolCall.functionName(), toolResult
                );
                conversationHistory.add(functionResponseMsg);
            }
        }

        // 7. 루프 초과 시 예외를 던진다
        throw new CustomException(CourseErrorCode.COURSE_GENERATION_LOOP_EXCEEDED);
    }

    /**
     * 1. AI의 도구 호출 요청을 실행한다
     * 2. 현재는 search_nearby_places만 지원한다
     * 3. 새 도구 추가 시 여기에 분기를 추가한다
     */
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

        log.info("[CourseService] 도구 실행: {} (lat={}, lng={}, category={}, radius={})",
            toolCall.functionName(), lat, lng, category, radius);

        return googlePlacesService.searchNearbyPlaces(lat, lng, category, radius);
    }

    // ============================================================
    // Private: 엔티티 저장 / 변환
    // ============================================================

    /**
     * 1. 코스 요청 정보를 DB에 저장한다
     * 2. DTO → Entity 변환 후 PENDING 상태로 생성한다
     */
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

        return courseRequestRepository.save(courseRequest);
    }

    /**
     * 1. Gemini가 생성한 JSON을 파싱하여 Course + CoursePlace를 저장한다
     * 2. JSON 파싱 실패 시 COURSE_GENERATION_FAILED 예외를 던진다
     */
    private Course parseCourseAndSave(String courseJson, CourseRequest courseRequest, Member member) {
        try {
            // 1. JSON에서 마크다운 코드블록 구분자를 제거한다
            String cleanJson = courseJson.replaceAll("```json|```", "").trim();
            JsonObject parsed = JsonParser.parseString(cleanJson).getAsJsonObject();

            // 2. Course 엔티티를 생성한다
            JsonArray placesArray = parsed.getAsJsonArray("places");
            int totalDuration = 0;
            int totalCostMin = 0;
            int totalCostMax = 0;

            // 3. 비용/시간 합계를 먼저 계산한다
            for (JsonElement el : placesArray) {
                JsonObject p = el.getAsJsonObject();
                totalDuration += p.has("duration_minutes") ? p.get("duration_minutes").getAsInt() : 0;
                totalDuration += p.has("travel_time_to_next") ? p.get("travel_time_to_next").getAsInt() : 0;
                totalCostMin += p.has("cost_min") ? p.get("cost_min").getAsInt() : 0;
                totalCostMax += p.has("cost_max") ? p.get("cost_max").getAsInt() : 0;
            }

            Course course = Course.builder()
                .courseRequest(courseRequest)
                .member(member)
                .title(parsed.has("title") ? parsed.get("title").getAsString() : "AI 추천 코스")
                .description(parsed.has("description") ? parsed.get("description").getAsString() : null)
                .totalDuration(totalDuration)
                .totalCostMin(totalCostMin)
                .totalCostMax(totalCostMax)
                .build();

            Course savedCourse = courseRepository.save(course);

            // 4. 각 장소를 CoursePlace로 저장한다
            for (JsonElement el : placesArray) {
                JsonObject p = el.getAsJsonObject();
                saveCoursePlaceFromJson(p, savedCourse);
            }

            return savedCourse;

        } catch (Exception e) {
            log.error("[CourseService] AI 응답 파싱 실패: {}", courseJson, e);
            throw new CustomException(CourseErrorCode.COURSE_GENERATION_FAILED);
        }
    }

    /**
     * 1. JSON의 개별 장소를 Place + CoursePlace로 저장한다
     * 2. google_place_id가 있으면 기존 Place를 조회, 없으면 신규 생성한다
     */
    private void saveCoursePlaceFromJson(JsonObject placeJson, Course course) {
        String googlePlaceId = placeJson.has("google_place_id")
            ? placeJson.get("google_place_id").getAsString() : null;

        // 1. Place 엔티티를 조회하거나 생성한다
        Place place;
        if (googlePlaceId != null) {
            place = placeRepository.findByGooglePlaceId(googlePlaceId)
                .orElseGet(() -> placeRepository.save(
                    Place.builder()
                        .googlePlaceId(googlePlaceId)
                        .name(placeJson.has("name") ? placeJson.get("name").getAsString() : "이름 없음")
                        .address("")
                        .latitude(java.math.BigDecimal.ZERO)
                        .longitude(java.math.BigDecimal.ZERO)
                        .category(parsePlaceCategory(placeJson))
                        .build()
                ));
        } else {
            place = placeRepository.save(
                Place.builder()
                    .googlePlaceId("generated-" + UUID.randomUUID())
                    .name(placeJson.has("name") ? placeJson.get("name").getAsString() : "이름 없음")
                    .address("")
                    .latitude(java.math.BigDecimal.ZERO)
                    .longitude(java.math.BigDecimal.ZERO)
                    .category(parsePlaceCategory(placeJson))
                    .build()
            );
        }

        // 2. CoursePlace를 생성하여 코스와 연결한다
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

    /**
     * 1. JSON 문자열을 PlaceCategory enum으로 변환한다
     * 2. 매칭 실패 시 ETC를 반환한다
     */
    private PlaceCategory parsePlaceCategory(JsonObject placeJson) {
        if (!placeJson.has("category")) {
            return PlaceCategory.ETC;
        }
        try {
            return PlaceCategory.valueOf(placeJson.get("category").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlaceCategory.ETC;
        }
    }

    /**
     * 1. JSON 문자열을 TravelMethod enum으로 변환한다
     * 2. 매칭 실패 시 NONE을 반환한다
     */
    private TravelMethod parseTravelMethod(JsonObject placeJson) {
        if (!placeJson.has("travel_method_to_next")) {
            return TravelMethod.NONE;
        }
        try {
            return TravelMethod.valueOf(placeJson.get("travel_method_to_next").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TravelMethod.NONE;
        }
    }

    /**
     * 1. Course 엔티티에서 CourseResponse DTO를 조립한다
     * 2. CoursePlace를 순서대로 조회하여 CoursePlaceResponse로 변환한다
     */
    private CourseResponse buildCourseResponse(Course course) {
        List<CoursePlace> coursePlaces = coursePlaceRepository.findByCourseOrderByOrderIndexAsc(course);

        List<CoursePlaceResponse> placeResponses = coursePlaces.stream()
            .map(cp -> CoursePlaceResponse.from(cp, cp.getPlace()))
            .toList();

        return CourseResponse.of(course, placeResponses);
    }
}
