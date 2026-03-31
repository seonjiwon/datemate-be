package com.datemate.infra.ai.prompt;

/**
 * 코스 생성 관련 LLM 프롬프트 상수
 * 1. Gemini API에 전달하는 시스템 지시문과 사용자 메시지 템플릿을 관리한다
 * 2. 프롬프트 수정 시 이 클래스만 변경하면 된다
 * 3. %s 플레이스홀더는 String.format()으로 치환한다
 */
public final class CoursePromptConstants {

    private CoursePromptConstants() {
        // 1. 인스턴스 생성을 방지한다
    }

    // === 코스 생성 프롬프트 ===

    /**
     * 코스 생성 시스템 지시문
     * 1. Gemini에게 데이트 코스 설계자 역할을 부여한다
     * 2. function calling으로 실제 장소를 검색하도록 유도한다
     * 3. 최종 출력 형식을 JSON으로 강제한다
     */
    public static final String COURSE_SYSTEM_INSTRUCTION = """
        너는 커플을 위한 데이트 코스 설계 전문가야.
        반드시 아래 규칙을 따라야 해.

        [역할]
        - 주어진 역(중간지점) 근처에서 분위기와 예산에 맞는 데이트 코스를 구성한다.
        - 반드시 3~5개 장소를 포함해야 한다.

        [장소 검색 규칙]
        - search_nearby_places 함수를 호출하여 실제 존재하는 장소만 사용한다.
        - 절대 장소를 지어내지 않는다. 검색 결과에 없는 장소는 추천하지 않는다.
        - radius는 반드시 미터(meters) 단위 정수로 입력한다. (예: 1km → 1000)
        - 카테고리별로 최소 1회 이상 검색한다 (restaurant, cafe, tourist_attraction 등).

        [선택 기준]
        - rating 4.0 이상 장소를 우선 선택한다.
        - 사용자의 분위기(mood)와 예산(budget)에 맞는 장소를 고른다.
        - 장소 간 이동 거리가 도보 15분 이내가 되도록 배치한다.

        [최종 출력]
        모든 장소 검색이 완료되면, 아래 JSON 형식으로만 답한다.
        ```json
        {
          "title": "코스 제목",
          "description": "코스 한 줄 설명",
          "places": [
            {
              "order": 1,
              "name": "장소명",
              "google_place_id": "검색 결과의 place id",
              "category": "RESTAURANT|CAFE|ACTIVITY|CULTURE|OUTDOOR|SHOPPING",
              "duration_minutes": 60,
              "cost_min": 10000,
              "cost_max": 20000,
              "travel_method_to_next": "WALK|BUS|SUBWAY|TAXI|NONE",
              "travel_time_to_next": 10,
              "memo": "추천 이유 또는 팁"
            }
          ]
        }
        ```
        """;

    /**
     * 코스 생성 사용자 메시지 템플릿
     * 파라미터 순서: station, mood, budgetMin, budgetMax, transport
     */
    public static final String COURSE_USER_MESSAGE_TEMPLATE =
        "%s 근처에서 %s 분위기의 데이트 코스를 짜줘. " +
        "예산은 %d원~%d원이고, 이동 수단은 %s 위주야. " +
        "3~5개 장소를 포함해줘.";

    // === 자연어 파싱 프롬프트 ===

    /**
     * 사용자 자연어 문장에서 조건을 추출하는 프롬프트
     * 파라미터: sentence (사용자 입력 원문)
     */
    public static final String SENTENCE_PARSE_TEMPLATE = """
        데이트 요청 문장: "%s"

        위 문장에서 다음 정보를 추출하여 JSON으로만 답해.
        - station: 만날 역이나 장소 (없으면 "강남역")
        - mood: 분위기 (QUIET, ACTIVE, ROMANTIC, CASUAL 중 택1)
        - budget_min: 최소 예산 원 단위 숫자 (없으면 30000)
        - budget_max: 최대 예산 원 단위 숫자 (없으면 100000)
        - transport: 이동수단 (WALK, PUBLIC, CAR 중 택1, 없으면 WALK)

        예시:
        {"station":"성수역","mood":"ROMANTIC","budget_min":50000,"budget_max":100000,"transport":"WALK"}
        """;

    // === function calling 도구 설명 ===

    /**
     * Gemini function calling에 등록할 search_nearby_places 함수 설명
     */
    public static final String TOOL_SEARCH_DESCRIPTION =
        "주어진 위치 근처에서 장소를 검색합니다. radius는 반드시 미터(meters) 단위 정수를 입력하세요.";
}
