package rw.martinhardware.mymartin.network;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rw.martinhardware.mymartin.BuildConfig;

/**
 * Central Retrofit singleton for the Rinjora (Kazinduzi) game API.
 *
 * - Adds the {@link AuthInterceptor} to attach the Bearer token.
 * - Logs HTTP bodies in debug builds only (never leaks the confidential
 *   {@code answer} field in release).
 * - Uses {@link ApiConfig#KAZINDUZI_BASE_URL}, a build-type aware base URL.
 *
 * Usage: {@code RinjoraApiClient.get(context).api()}
 */
public final class RinjoraApiClient {

    private final Retrofit retrofit;
    private final RinjoraApi api;

    private RinjoraApiClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.KAZINDUZI_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(RinjoraApi.class);
    }

    private static volatile RinjoraApiClient instance;

    public static RinjoraApiClient get(Context context) {
        if (instance == null) {
            synchronized (RinjoraApiClient.class) {
                if (instance == null) {
                    instance = new RinjoraApiClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public RinjoraApi api() {
        return api;
    }
}
