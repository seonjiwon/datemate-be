package com.datemate.domain.place.service;

import com.datemate.domain.place.code.PlaceErrorCode;
import com.datemate.domain.place.entity.Place;
import com.datemate.domain.place.entity.enums.PlaceCategory;
import com.datemate.domain.place.repository.PlaceRepository;
import com.datemate.global.exception.CustomException;
import com.datemate.global.util.RetryUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Google Places API (New) 서비스
 * 1. 좌표 기반 주변 장소를 검색하고, 결과를 Place 엔티티로 캐싱한다
 * 2. API 호출과 DB 캐싱을 분리하여 각각 독립 테스트 가능하다
 * 3. CourseService의 에이전트 루프에서 도구 실행기로 사용된다
 */
@Slf4j
@Service
public class GooglePlacesService {

    private final RestClient restClient;
    private final PlaceRepository placeRepository;
    private final RetryUtil retryUtil;
    private final String apiKey;

    /**
     * 1. googlePlacesRestClient 빈과 의존성을 주입받는다
     * 2. RetryUtil은 new로 생성 — 빈 등록하지 않아 테스트에서 교체 용이하다
     * 3. @Autowired 명시 — 테스트용 생성자와 구별하기 위해 필수
     */
    @Autowired
    public GooglePlacesService(
        @Qualifier("googlePlacesRestClient") RestClient restClient,
        PlaceRepository placeRepository,
        @Value("${google.api.key}") String apiKey
    ) {
        this.restClient = restClient;
        this.placeRepository = placeRepository;
        this.retryUtil = new RetryUtil();
        this.apiKey = apiKey;
    }

    // 테스트용 생성자 — RetryUtil을 외부에서 주입할 수 있다
    GooglePlacesService(
        RestClient restClient,
        PlaceRepository placeRepository,
        String apiKey,
        RetryUtil retryUtil
    ) {
        this.restClient = restClient;
        this.placeRepository = placeRepository;
        this.apiKey = apiKey;
        this.retryUtil = retryUtil;
    }

    /**
     * 1. 좌표 기반으로 주변 장소를 검색한다 (Google Places API 호출)
     * 2. 검색 결과를 JSON 문자열로 반환한다 — GeminiClient에 피드백할 원본 데이터
     * 3. 429 응답 시 선형 백오프로 재시도한다
     *
     * @param lat 검색 중심 위도
     * @param lng 검색 중심 경도
     * @param category 장소 카테고리 (restaurant, cafe 등)
     * @param radius 검색 반경 (미터)
     * @return Google Places API 응답 JSON 문자열
     */
    public String searchNearbyPlaces(double lat, double lng, String category, double radius) {
        log.info("[GooglePlacesService] 주변 장소 검색 시작: lat={}, lng={}, category={}, radius={}", lat, lng, category, radius);

        // 1. radius가 10 미만이면 km 단위로 오해한 것 — 미터로 보정한다
        double correctedRadius = radius < 10 ? radius * 1000 : radius;
        if (correctedRadius != radius) {
            log.debug("[GooglePlacesService] 반경 보정: {}→{}m (km→m 변환)", radius, correctedRadius);
        }

        // 2. 요청 본문을 조립한다
        String requestBody = buildSearchRequestBody(lat, lng, category, correctedRadius);
        log.debug("[GooglePlacesService] 요청 본문: {}", requestBody);

        // 3. 429 대비 재시도 로직으로 API를 호출한다
        long startTime = System.currentTimeMillis();
        String result = retryUtil.executeWithRetry(
            () -> callPlacesApi(requestBody),
            "GooglePlaces.searchNearby"
        );
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("[GooglePlacesService] 주변 장소 검색 완료 (소요시간: {}ms, 응답크기: {}자)", elapsed, result != null ? result.length() : 0);
        log.debug("[GooglePlacesService] 응답 미리보기: {}", result != null && result.length() > 500 ? result.substring(0, 500) + "..." : result);

        return result;
    }

    /**
     * 1. Google Places API 검색 결과를 Place 엔티티로 변환하여 DB에 캐싱한다
     * 2. 이미 존재하는 장소는 건너뛰고, 신규 장소만 저장한다
     * 3. 코스 확정 시 호출되어 코스에 포함된 장소를 영속화한다
     *
     * @param placesJson Google Places API 응답 JSON
     * @return 저장된 Place 엔티티 목록
     */
    @Transactional
    public List<Place> cacheAndGetPlaces(String placesJson) {
        log.info("[GooglePlacesService] 장소 캐싱 시작");
        List<Place> result = new ArrayList<>();

        try {
            JsonObject response = JsonParser.parseString(placesJson).getAsJsonObject();

            if (!response.has("places")) {
                log.warn("[GooglePlacesService] API 응답에 'places' 필드가 없음 — 빈 결과 반환");
                return result;
            }

            JsonArray places = response.getAsJsonArray("places");
            log.info("[GooglePlacesService] 파싱된 장소 수: {}개", places.size());

            int cached = 0, newSaved = 0, skipped = 0;
            for (JsonElement element : places) {
                JsonObject placeObj = element.getAsJsonObject();
                String googlePlaceId = extractString(placeObj, "id");

                if (googlePlaceId == null) {
                    skipped++;
                    log.debug("[GooglePlacesService] googlePlaceId가 null인 장소 건너뜀");
                    continue;
                }

                // 1. 이미 캐싱된 장소면 DB에서 가져온다
                boolean[] isNew = {false};
                Place place = placeRepository.findByGooglePlaceId(googlePlaceId)
                    .orElseGet(() -> {
                        isNew[0] = true;
                        return savePlaceFromJson(placeObj, googlePlaceId);
                    });

                if (isNew[0]) {
                    newSaved++;
                    log.debug("[GooglePlacesService] 신규 장소 저장: id={}, name={}", place.getId(), place.getName());
                } else {
                    cached++;
                }
                result.add(place);
            }

            log.info("[GooglePlacesService] 장소 캐싱 완료: 기존={}건, 신규={}건, 건너뜀={}건", cached, newSaved, skipped);
        } catch (Exception e) {
            log.error("[GooglePlacesService] 장소 캐싱 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(PlaceErrorCode.GOOGLE_API_PARSE_ERROR);
        }

        return result;
    }

    /**
     * 1. Google Places 검색 요청 본문을 JSON으로 조립한다
     * 2. locationRestriction + includedTypes 구조를 사용한다
     */
    private String buildSearchRequestBody(double lat, double lng, String category, double radius) {
        JsonObject center = new JsonObject();
        center.addProperty("latitude", lat);
        center.addProperty("longitude", lng);

        JsonObject circle = new JsonObject();
        circle.add("center", center);
        circle.addProperty("radius", radius);

        JsonObject locationRestriction = new JsonObject();
        locationRestriction.add("circle", circle);

        JsonObject body = new JsonObject();
        body.add("locationRestriction", locationRestriction);

        if (category != null && !category.trim().isEmpty()) {
            JsonArray types = new JsonArray();
            types.add(category);
            body.add("includedTypes", types);
        }

        return body.toString();
    }

    /**
     * 1. Google Places API를 실제 호출한다
     * 2. RestClient 기반 동기 호출 — WebClient의 .block() 패턴 대체
     */
    private String callPlacesApi(String requestBody) {
        log.debug("[GooglePlacesService] Places API 호출: POST /v1/places:searchNearby");
        try {
            String response = restClient.post()
                             .uri("/v1/places:searchNearby")
                             .header("X-Goog-Api-Key", apiKey)
                             .header("X-Goog-FieldMask",
                                 "places.id,places.displayName,places.formattedAddress," +
                                 "places.location,places.rating,places.userRatingCount," +
                                 "places.priceLevel,places.photos")
                             .header("Content-Type", "application/json")
                             .body(requestBody)
                             .retrieve()
                             .body(String.class);
            log.debug("[GooglePlacesService] Places API 응답 수신 완료");
            return response;
        } catch (Exception e) {
            log.error("[GooglePlacesService] Places API 호출 실패: {}", e.getMessage(), e);
            throw new CustomException(PlaceErrorCode.GOOGLE_API_ERROR);
        }
    }

    /**
     * 1. Google Places JSON 응답을 Place 엔티티로 변환하여 저장한다
     * 2. 신규 장소에 대해서만 호출된다
     */
    private Place savePlaceFromJson(JsonObject placeObj, String googlePlaceId) {
        JsonObject location = placeObj.has("location") ? placeObj.getAsJsonObject("location") : null;
        JsonObject displayName = placeObj.has("displayName") ? placeObj.getAsJsonObject("displayName") : null;

        Place place = Place.builder()
            .googlePlaceId(googlePlaceId)
            .name(displayName != null ? extractString(displayName, "text") : "이름 없음")
            .address(extractString(placeObj, "formattedAddress"))
            .latitude(location != null ? BigDecimal.valueOf(location.get("latitude").getAsDouble()) : BigDecimal.ZERO)
            .longitude(location != null ? BigDecimal.valueOf(location.get("longitude").getAsDouble()) : BigDecimal.ZERO)
            .category(PlaceCategory.ETC)
            .rating(placeObj.has("rating") ? BigDecimal.valueOf(placeObj.get("rating").getAsDouble()) : null)
            .priceLevel(placeObj.has("priceLevel") ? placeObj.get("priceLevel").getAsInt() : null)
            .photoReference(extractPhotoReference(placeObj))
            .build();

        return placeRepository.save(place);
    }

    /**
     * 1. JSON 객체에서 문자열 값을 안전하게 추출한다
     * 2. null 체크를 중앙화하여 NullPointerException을 방지한다
     */
    private String extractString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
            ? obj.get(key).getAsString()
            : null;
    }

    /**
     * 1. 장소의 첫 번째 사진 참조 키를 추출한다
     * 2. 사진이 없으면 null을 반환한다
     */
    private String extractPhotoReference(JsonObject placeObj) {
        if (!placeObj.has("photos")) {
            return null;
        }
        JsonArray photos = placeObj.getAsJsonArray("photos");
        if (photos.isEmpty()) {
            return null;
        }
        JsonObject firstPhoto = photos.get(0).getAsJsonObject();
        return extractString(firstPhoto, "name");
    }
}
