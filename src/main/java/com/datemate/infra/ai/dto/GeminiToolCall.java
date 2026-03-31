package com.datemate.infra.ai.dto;

import com.google.gson.JsonObject;

/**
 * Gemini function calling 응답을 담는 DTO
 * 1. AI가 도구 호출을 요청했을 때 함수명과 인자를 구조화한다
 * 2. CourseService에서 어떤 외부 API를 호출할지 결정하는 기준이 된다
 *
 * @param functionName 호출할 함수 이름 (ex: "search_nearby_places")
 * @param arguments 함수에 전달할 인자 (lat, lng, category, radius 등)
 */
public record GeminiToolCall(
    String functionName,
    JsonObject arguments
) {
}
