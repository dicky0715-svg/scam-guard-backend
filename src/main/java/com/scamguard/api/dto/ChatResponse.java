package com.scamguard.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ChatResponse(
        List<Choice> choices
) {
    public record Choice(
            @JsonProperty("index") int index,
            @JsonProperty("message") ChatMessage message
    ) {}

    public String getFirstReplyContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).message().content();
        }
        return null;
    }
}