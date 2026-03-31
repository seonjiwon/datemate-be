package com.datemate.global.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 선형 백오프 + Jitter 재시도 유틸리티
 * 1. 외부 API 호출 시 429(Too Many Requests) 대응용이다
 * 2. 선형 백오프: 재시도마다 baseDelay만큼 대기 시간이 증가한다 (1초 → 2초 → 3초)
 * 3. Jitter: 랜덤 0~500ms를 추가하여 동시 재시도 충돌을 방지한다
 * 4. static 메서드가 아닌 인스턴스 메서드로 설계하여 테스트 시 spy/mock 가능하다
 */
@Slf4j
public class RetryUtil {

    // 기본 대기 단위 (밀리초)
    private final long baseDelayMs;
    // 최대 재시도 횟수
    private final int maxRetries;
    // Jitter 최대 범위 (밀리초)
    private final long maxJitterMs;

    /**
     * 1. 기본 설정으로 생성한다 (1초 base, 3회 재시도, 500ms jitter)
     * 2. MVP 기준 Gemini 무료 티어 rate limit에 적합한 값이다
     */
    public RetryUtil() {
        this(1_000L, 3, 500L);
    }

    /**
     * 1. 커스텀 설정으로 생성한다
     * 2. 테스트에서 짧은 delay로 빠르게 검증할 때 사용한다
     */
    public RetryUtil(long baseDelayMs, int maxRetries, long maxJitterMs) {
        this.baseDelayMs = baseDelayMs;
        this.maxRetries = maxRetries;
        this.maxJitterMs = maxJitterMs;
    }

    /**
     * 1. 주어진 작업을 실행하고, 429 응답 시 선형 백오프로 재시도한다
     * 2. 429가 아닌 예외는 즉시 던진다 — 재시도 대상이 아니다
     * 3. 최대 재시도 횟수를 초과하면 마지막 예외를 그대로 던진다
     *
     * @param operation 실행할 작업 (Supplier로 감싸서 전달)
     * @param operationName 로깅용 작업 이름
     * @return 작업 결과
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;

        while (true) {
            try {
                return operation.get();
            } catch (HttpClientErrorException e) {
                // 1. 429(Too Many Requests)만 재시도 대상이다
                if (e.getStatusCode().value() != 429 || attempt >= maxRetries) {
                    throw e;
                }

                attempt++;
                long delay = calculateDelay(attempt);

                log.warn("[RetryUtil] {} 429 발생 — {}회 재시도, {}ms 대기",
                    operationName, attempt, delay);

                sleep(delay);
            }
        }
    }

    /**
     * 1. 선형 백오프 + Jitter 대기 시간을 계산한다
     * 2. delay = baseDelay * attempt + random(0, maxJitter)
     * 3. attempt=1이면 1초+jitter, attempt=2이면 2초+jitter, ...
     */
    long calculateDelay(int attempt) {
        long linearDelay = baseDelayMs * attempt;
        long jitter = ThreadLocalRandom.current().nextLong(0, maxJitterMs + 1);
        return linearDelay + jitter;
    }

    /**
     * 1. Thread.sleep을 감싼 메서드 — 테스트에서 오버라이드 가능하다
     * 2. protected 접근자로 테스트 서브클래스에서 sleep을 no-op으로 대체할 수 있다
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 대기 중 인터럽트 발생", e);
        }
    }
}
