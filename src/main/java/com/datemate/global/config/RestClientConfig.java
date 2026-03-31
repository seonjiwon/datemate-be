package com.datemate.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient 빈 설정
 * 1. 용도별 RestClient를 분리하여 타임아웃과 base URL을 독립 관리한다
 * 2. 테스트 시 각 RestClient를 개별 모킹할 수 있다
 */
@Configuration
public class RestClientConfig {

    @Value("${gemini.api.url}")
    private String geminiBaseUrl;

    @Value("${google.places.api.url}")
    private String googlePlacesBaseUrl;

    /**
     * 1. Gemini API 전용 RestClient를 생성한다
     * 2. LLM 응답은 느릴 수 있으므로 타임아웃을 60초로 설정한다
     */
    @Bean(name = "geminiRestClient")
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        return RestClient.builder()
                         .baseUrl(geminiBaseUrl)
                         .requestFactory(factory)
                         .build();
    }

    /**
     * 1. Google Places API 전용 RestClient를 생성한다
     * 2. Places API는 응답이 빠르므로 타임아웃을 10초로 설정한다
     */
    @Bean(name = "googlePlacesRestClient")
    public RestClient googlePlacesRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);

        return RestClient.builder()
                         .baseUrl(googlePlacesBaseUrl)
                         .requestFactory(factory)
                         .build();
    }
}
