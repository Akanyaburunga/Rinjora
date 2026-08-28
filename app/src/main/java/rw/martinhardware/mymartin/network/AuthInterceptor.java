package rw.martinhardware.mymartin.network;

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
        String token = AuthTokenStore.get(context).getToken();
        if (token != null && !token.isEmpty()) {
            request = request.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }
        return chain.proceed(request);
    }
}
