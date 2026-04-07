package com.scamguard.api.dto;

public record ChatMessage(
        String role,
        String content
) {}