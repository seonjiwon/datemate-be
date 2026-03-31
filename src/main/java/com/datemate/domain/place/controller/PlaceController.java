package com.datemate.domain.place.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * 장소 API 컨트롤러
 * 1. Google Places 사진 프록시 엔드포인트를 제공한다
 * 2. API 키를 프론트엔드에 노출하지 않기 위해 백엔드에서 프록시한다
 * 3. Cache-Control 헤더로 브라우저/앱 캐싱을 활용한다
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    @Value("${google.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    /**
     * GET /api/v1/places/photo?ref={photoReference}
     * 1. Google Places Photo API를 호출하여 이미지를 반환한다
     * 2. photoReference는 "places/{placeId}/photos/{photoId}" 형태이다
     * 3. skipHttpRedirect=true로 이미지 바이트를 직접 받는다
     *
     * @param ref Google Places photo resource name
     * @return JPEG 이미지 바이트
     */
    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPhoto(@RequestParam String ref) {
        log.debug("[PlaceController] GET /places/photo — 사진 프록시 요청: ref={}", ref);

        try {
            long startTime = System.currentTimeMillis();

            String googleUrl = String.format(
                "https://places.googleapis.com/v1/%s/media?maxHeightPx=400&maxWidthPx=400&key=%s&skipHttpRedirect=true",
                ref, apiKey
            );

            byte[] imageBytes = restClient.get()
                .uri(googleUrl)
                .retrieve()
                .body(byte[].class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[PlaceController] GET /places/photo — 사진 로드 완료 ({}ms, {}bytes)", elapsed,
                imageBytes != null ? imageBytes.length : 0);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(imageBytes);

        } catch (Exception e) {
            log.error("[PlaceController] GET /places/photo — 사진 로드 실패: ref={}, error={}", ref, e.getMessage());

            // 실패 시 204 No Content 반환 (프론트에서 빈 이미지 처리)
            return ResponseEntity.noContent().build();
        }
    }
}
