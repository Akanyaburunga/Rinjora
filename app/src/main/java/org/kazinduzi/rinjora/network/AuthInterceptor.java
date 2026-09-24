package org.kazinduzi.rinjora.network;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Attaches {@code Authorization: Bearer <token>} to every outbound Rinjora API
 * request when a token is present.
 */
public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        // Laravel returns 401 JSON only when the request "expects JSON". Without this
        // header it redirects unauthenticated/unverified API calls to the HTML /login
        // page, which OkHttp follows and Gson then fails to parse (MalformedJsonException).
        request = request.newBuilder()
                .header("Accept", "application/json")
                .build();
        String token = AuthTokenStore.get(context).getToken();
        if (token != null && !token.isEmpty()) {
            request = request.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }
        okhttp3.Response response = chain.proceed(request);
        if (response.code() == 401) {
            // A 401 means the Bearer token is definitively invalid (expired/revoked).
            // Discard it centrally so the next app entry lands on the login form instead
            // of RinjoraAuthViewModel#checkAuthStatus bouncing an unexpired-but-dead
            // token straight back into RinjoraHomeActivity in an endless loop.
            AuthTokenStore.get(context).clear();
        } else if (response.code() == 403) {
            response = handleForbidden(response);
        }
        return response;
    }

    /**
     * A 403 with {@code "Your email address is not verified."} means the token is
     * valid but gated (docs mobile-api-email-verification.md §3.3). We drop the
     * token (so {@link AuthTokenStore#hasValidToken()} no longer bounces the user
     * back into the gated activity), flag the account and normalize the response to
     * a 401 so every existing {@code code() == 401 → onAuthError()} path routes the
     * user to the auth host — which then opens the Enter Code screen.
     */
    private Response handleForbidden(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            return response;
        }
        String raw;
        try {
            raw = body.string();
        } catch (IOException e) {
            // Leave the response untouched; the caller will handle parsing.
            return response;
        }
        boolean unverified = raw.toLowerCase().contains("email address is not verified");
        if (unverified) {
            AuthTokenStore store = AuthTokenStore.get(context);
            store.clear();
            store.setEmailVerificationRequired(true);
        }
        // Rebuild the response: we consumed the body stream just now, so Gson must
        // receive a fresh one downstream.
        return response.newBuilder()
                .code(unverified ? 401 : 403)
                .message(response.message())
                .body(ResponseBody.create(raw, body.contentType()))
                .build();
    }
}
