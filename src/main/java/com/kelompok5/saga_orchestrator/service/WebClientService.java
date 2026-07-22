package com.kelompok5.saga_orchestrator.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WebClientService {

    private final WebClient webClient;

    public WebClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Map<String, Object> getMock1() {
        return getMock("/mock1");
    }

    public Map<String, Object> getMock2() {
        return getMock("/mock2");
    }

    public Map<String, Object> getMock2(boolean fail) {
        String queryUri = fail ? "/mock2?fail=true" : "/mock2";
        return webClient.get()
                .uri(queryUri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> getMock3() {
        return getMock("/mock3");
    }

    private Map<String, Object> getMock(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> cancelOrder() {
        return webClient.post()
                .uri("/mock1/cancel")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> refundPayment() {
        return webClient.post()
                .uri("/mock2/refund")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
