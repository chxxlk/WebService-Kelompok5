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
        return getMock("/mock1", false);
    }

    public Map<String, Object> getMock1(boolean fail) {
        return getMock("/mock1", fail);
    }

    public Map<String, Object> getMock2() {
        return getMock("/mock2", false);
    }

    public Map<String, Object> getMock2(boolean fail) {
        return getMock("/mock2", fail);
    }

    public Map<String, Object> getMock3() {
        return getMock("/mock3", false);
    }

    public Map<String, Object> getMock3(boolean fail) {
        return getMock("/mock3", fail);
    }

    private Map<String, Object> getMock(String uri, boolean fail) {
        String queryUri = fail ? uri + "?fail=true" : uri;
        return webClient.get()
                .uri(queryUri)
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

    public Map<String, Object> restockInventory() {
        return webClient.post()
                .uri("/mock3/restock")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
