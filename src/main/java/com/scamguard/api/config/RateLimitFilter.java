package com.scamguard.api.config;

import com.bucket4j.Bucket;
import com.bucket4j.ConsumptionProbe;
import com.bucket4j.Bandwidth;
import com.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    // 儲存每個 IP 嘅 bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 每個 IP 每分鐘最多 10 個 request
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> createNewBucket());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只對 /api/* 路徑做限流
        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // 獲取用戶 IP
        String ip = httpRequest.getRemoteAddr();

        // 如果有 proxy，用 X-Forwarded-For
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            ip = xForwardedFor.split(",")[0].trim();
        }

        Bucket bucket = getBucket(ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // 未超過限制，繼續處理
            httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            // 超過限制，回傳 429 Too Many Requests
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("""
                {
                    "error": "Too many requests",
                    "message": "你已超過每分鐘 10 次嘅查詢限制，請稍後再試。",
                    "retryAfterSeconds": 60
                }
                """);
        }
    }
}