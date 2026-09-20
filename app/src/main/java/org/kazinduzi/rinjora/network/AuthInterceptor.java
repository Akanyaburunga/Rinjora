package org.kazinduzi.rinjora.network;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

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
        // A 401 means the Bearer token is definitively invalid (expired/revoked).
        // Discard it centrally so the next app entry lands on the login form instead
        // of RinjoraAuthViewModel#checkAuthStatus bouncing an unexpired-but-dead
        // token straight back into RinjoraHomeActivity in an endless loop.
        if (response.code() == 401) {
            AuthTokenStore.get(context).clear();
        }
        return response;
    }
}
