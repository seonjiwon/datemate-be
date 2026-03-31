package com.datemate.infra.ai;

import com.datemate.infra.ai.dto.GeminiResponse;
import com.datemate.infra.ai.dto.GeminiToolCall;
import com.datemate.infra.ai.prompt.CoursePromptConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gemini API 통신 클라이언트
 * 1. RestClient 기반으로 Gemini API와 HTTP 통신을 담당한다
 * 2. JSON 요청/응답 조립만 처리하고, 비즈니스 로직은 포함하지 않는다
 * 3. CourseService가 이 클라이언트를 주입받아 에이전트 루프를 구동한다
 * 4. 테스트 시 이 클래스만 모킹하면 CourseService를 독립 테스트할 수 있다
 */
@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    /**
     * 1. geminiRestClient 빈과 설정값을 주입받는다
     * 2. @Qualifier로 용도별 RestClient를 구분한다
     */
    public GeminiClient(
        @Qualifier("geminiRestClient") RestClient restClient,
        @Value("${gemini.api.key}") String apiKey,
        @Value("${gemini.api.model}") String model
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 1. Gemini API에 대화 메시지를 전송하고 응답을 받는다
     * 2. 시스템 지시문, 대화 이력, 도구 스키마를 조립하여 전송한다
     * 3. 응답을 GeminiResponse로 정규화하여 반환한다
     *
     * @param systemInstruction 시스템 역할 지시문
     * @param conversationHistory 대화 이력 JSON 문자열 리스트
     * @return 정규화된 응답 (텍스트 또는 도구 호출)
     */
    public GeminiResponse chat(String systemInstruction, List<String> conversationHistory) {
        // 1. 요청 본문을 조립한다
        JsonObject requestBody = buildRequestBody(systemInstruction, conversationHistory);

        // 2. Gemini API에 POST 요청을 전송한다
        String endpoint = String.format(
            "/v1beta/models/%s:generateContent?key=%s", model, apiKey
        );

        String responseStr = restClient.post()
                                       .uri(endpoint)
                                       .header("Content-Type", "application/json")
                                       .body(requestBody.toString())
                                       .retrieve()
                                       .body(String.class);

        // 3. 응답을 파싱하여 GeminiResponse로 변환한다
        return parseResponse(responseStr);
    }

    /**
     * 1. Gemini API 요청 본문을 조립한다
     * 2. contents, system_instruction, tools 세 섹션을 포함한다
     */
    private JsonObject buildRequestBody(String systemInstruction, List<String> history) {
        JsonObject body = new JsonObject();

        // 1. 대화 이력을 contents 배열로 변환한다
        JsonArray contents = new JsonArray();
        for (String messageJson : history) {
            contents.add(JsonParser.parseString(messageJson));
        }
        body.add("contents", contents);

        // 2. 시스템 지시문을 추가한다
        JsonObject sysInst = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text", systemInstruction);
        sysParts.add(sysText);
        sysInst.add("parts", sysParts);
        body.add("system_instruction", sysInst);

        // 3. function calling 도구 스키마를 추가한다
        body.add("tools", buildToolsSchema());

        return body;
    }

    /**
     * 1. Gemini에 등록할 search_nearby_places 함수 스키마를 생성한다
     * 2. AI가 이 스키마를 보고 적절한 시점에 함수 호출을 요청한다
     */
    private JsonArray buildToolsSchema() {
        JsonObject latProp = new JsonObject();
        latProp.addProperty("type", "NUMBER");
        latProp.addProperty("description", "검색 중심 위도");

        JsonObject lngProp = new JsonObject();
        lngProp.addProperty("type", "NUMBER");
        lngProp.addProperty("description", "검색 중심 경도");

        JsonObject categoryProp = new JsonObject();
        categoryProp.addProperty("type", "STRING");
        categoryProp.addProperty("description", "장소 카테고리 (restaurant, cafe, tourist_attraction 등)");

        JsonObject radiusProp = new JsonObject();
        radiusProp.addProperty("type", "NUMBER");
        radiusProp.addProperty("description", "검색 반경 (미터 단위 정수, 예: 1000)");

        JsonObject properties = new JsonObject();
        properties.add("lat", latProp);
        properties.add("lng", lngProp);
        properties.add("category", categoryProp);
        properties.add("radius", radiusProp);

        JsonArray required = new JsonArray();
        required.add("lat");
        required.add("lng");
        required.add("category");
        required.add("radius");

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "OBJECT");
        parameters.add("properties", properties);
        parameters.add("required", required);

        JsonObject searchFunc = new JsonObject();
        searchFunc.addProperty("name", "search_nearby_places");
        searchFunc.addProperty("description", CoursePromptConstants.TOOL_SEARCH_DESCRIPTION);
        searchFunc.add("parameters", parameters);

        JsonArray functions = new JsonArray();
        functions.add(searchFunc);

        JsonObject tool = new JsonObject();
        tool.add("functionDeclarations", functions);

        JsonArray tools = new JsonArray();
        tools.add(tool);

        return tools;
    }

    /**
     * 1. Gemini API 응답 JSON을 GeminiResponse로 변환한다
     * 2. functionCall이면 GeminiToolCall로, text면 텍스트로 매핑한다
     * 3. rawModelContent에 원본 content를 보존하여 대화 이력에 추가할 수 있게 한다
     */
    private GeminiResponse parseResponse(String responseStr) {
        JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
        JsonObject content = response.getAsJsonArray("candidates")
                                     .get(0).getAsJsonObject()
                                     .getAsJsonObject("content");

        String rawContent = content.toString();
        JsonObject firstPart = content.getAsJsonArray("parts")
                                      .get(0).getAsJsonObject();

        // 1. function call 응답인지 확인한다
        if (firstPart.has("functionCall")) {
            JsonObject call = firstPart.getAsJsonObject("functionCall");
            GeminiToolCall toolCall = new GeminiToolCall(
                call.get("name").getAsString(),
                call.getAsJsonObject("args")
            );
            return new GeminiResponse(Optional.empty(), Optional.of(toolCall), rawContent);
        }

        // 2. 텍스트 응답이면 내용을 추출한다
        String text = firstPart.get("text").getAsString();
        return new GeminiResponse(Optional.of(text), Optional.empty(), rawContent);
    }

    /**
     * 1. function calling 결과를 Gemini 대화 이력 형식으로 변환한다
     * 2. AI에게 도구 실행 결과를 피드백할 때 사용한다
     *
     * @param functionName 실행한 함수 이름
     * @param resultData 함수 실행 결과 (JSON 문자열)
     * @return 대화 이력에 추가할 JSON 문자열
     */
    public String buildFunctionResponseMessage(String functionName, String resultData) {
        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("content", resultData);

        JsonObject functionResponse = new JsonObject();
        functionResponse.addProperty("name", functionName);
        functionResponse.add("response", responseObj);

        JsonObject part = new JsonObject();
        part.add("functionResponse", functionResponse);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject message = new JsonObject();
        message.addProperty("role", "function");
        message.add("parts", parts);

        return message.toString();
    }

    /**
     * 1. 사용자 메시지를 Gemini 대화 이력 형식으로 변환한다
     * 2. 에이전트 루프 시작 시 첫 메시지로 사용한다
     *
     * @param text 사용자 메시지 텍스트
     * @return 대화 이력에 추가할 JSON 문자열
     */
    public String buildUserMessage(String text) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", text);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.add("parts", parts);

        return message.toString();
    }
}
