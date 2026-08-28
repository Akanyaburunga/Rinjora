package org.kazinduzi.rinjora.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Retries transient failures with exponential backoff (plan Phase 10/K).
 *
 * Safe to repeat only idempotent HTTP methods ({@code GET}). On HTTP 429
 * (rate limited) it honours the server's {@code Retry-After} header and backs
 * off, doubling the delay up to {@link #MAX_BACKOFF_MS}; 5xx are also retried
 * with the same backoff a bounded number of times. Non-idempotent requests and
 * other status codes pass through untouched.
 */
public class RetryInterceptor implements Interceptor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long MAX_BACKOFF_MS = 8000;
    private static final long BASE_BACKOFF_MS = 500;

    private final int maxAttempts;

    public RetryInterceptor() {
        this(MAX_ATTEMPTS);
    }

    RetryInterceptor(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!"GET".equals(request.method()) && !"HEAD".equals(request.method())) {
            return chain.proceed(request);
        }

        Response response = null;
        IOException lastIo = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (response != null) {
                response.close();
            }
            try {
                response = chain.proceed(request);
            } catch (IOException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                lastIo = e;
                sleep(backoffMs(attempt));
                continue;
            }

            if (!isRetryable(response) || attempt == maxAttempts) {
                return response;
            }
            sleep(backoffMs(attempt, response));
        }
        if (lastIo != null) {
            throw lastIo;
        }
        return response;
    }

    private boolean isRetryable(Response response) {
        int code = response.code();
        return code == 429 || (code >= 500 && code < 600);
    }

    private long backoffMs(int attempt) {
        return Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L << (attempt - 1)));
    }

    private long backoffMs(int attempt, Response response) {
        return Math.min(MAX_BACKOFF_MS, delayFromRetryAfter(response, backoffMs(attempt)));
    }

    private static long delayFromRetryAfter(Response response, long fallback) {
        String header = response.header("Retry-After");
        if (header == null) {
            return fallback;
        }
        try {
            return Long.parseLong(header.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
