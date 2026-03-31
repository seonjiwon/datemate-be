package com.datemate.infra.ai.dto;

import java.util.Optional;

/**
 * Gemini API 응답을 정규화한 DTO
 * 1. Gemini 응답은 텍스트 or function call 두 가지 형태이다
 * 2. 이 DTO로 통일하여 CourseService가 Gemini JSON 구조에 직접 의존하지 않게 한다
 * 3. 테스트에서 이 DTO만 생성하면 GeminiClient를 모킹할 수 있다
 *
 * @param textContent AI가 최종 텍스트를 반환한 경우
 * @param toolCall AI가 함수 호출을 요청한 경우
 * @param rawModelContent 대화 이력에 추가할 원본 content 객체
 */
public record GeminiResponse(
    Optional<String> textContent,
    Optional<GeminiToolCall> toolCall,
    String rawModelContent
) {

    /**
     * 1. AI가 최종 텍스트 응답을 했는지 판단한다
     * 2. true면 에이전트 루프를 종료하고 결과를 파싱한다
     */
    public boolean isTextResponse() {
        return textContent.isPresent();
    }

    /**
     * 1. AI가 도구 호출을 요청했는지 판단한다
     * 2. true면 해당 도구를 실행하고 결과를 피드백한다
     */
    public boolean isToolCallResponse() {
        return toolCall.isPresent();
    }
}
