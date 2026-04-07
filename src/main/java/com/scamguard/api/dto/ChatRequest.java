package com.scamguard.api.dto;

import java.util.List;

public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Integer max_tokens
) {
    public ChatRequest(String model, List<ChatMessage> messages, Double temperature, Integer max_tokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
    }
}