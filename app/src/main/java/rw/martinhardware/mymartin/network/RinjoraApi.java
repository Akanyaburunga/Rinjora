package rw.martinhardware.mymartin.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import rw.martinhardware.mymartin.network.dto.CategoryDto;
import rw.martinhardware.mymartin.network.dto.LoginResponseDto;
import rw.martinhardware.mymartin.network.dto.RiddleDto;
import rw.martinhardware.mymartin.network.dto.SummaryDto;
import rw.martinhardware.mymartin.network.dto.UserDto;

/**
 * Rinjora (Kazinduzi) game API surface (plan §1–§3). Every method returns the
 * standard {@link ApiEnvelope} wrapper. Backend contract is in
 * {@code docs/android-app-implementation-plan.md}.
 */
public interface RinjoraApi {

    // ------------------------------------------------------------------
    // Auth (plan §1)
    // ------------------------------------------------------------------

    @POST("auth/register")
    Call<ApiEnvelope<Void>> register(@Body Map<String, Object> body);

    @POST("auth/login")
    Call<ApiEnvelope<LoginResponseDto>> login(@Body Map<String, Object> body);

    @POST("auth/logout")
    Call<ApiEnvelope<Void>> logout(@Body Map<String, Object> body);

    @GET("auth/user")
    Call<ApiEnvelope<UserDto>> currentUser();

    // ------------------------------------------------------------------
    // Profile summary / home (plan §4.1)
    // ------------------------------------------------------------------

    @GET("me/summary")
    Call<ApiEnvelope<SummaryDto>> summary();

    // ------------------------------------------------------------------
    // Riddles (plan §2, §3)
    // ------------------------------------------------------------------

    @GET("riddles")
    Call<ApiEnvelope<List<RiddleDto>>> riddles(@QueryMap Map<String, String> filters);

    @GET("riddles")
    Call<ApiEnvelope<List<RiddleDto>>> riddles();

    @GET("riddles/categories")
    Call<ApiEnvelope<List<CategoryDto>>> categories();

    @GET("riddles/{id}")
    Call<ApiEnvelope<RiddleDto>> riddle(@Path("id") long id);

    @GET("riddles/next")
    Call<ApiEnvelope<RiddleDto>> nextRiddle(@Query("difficulty") String difficulty);
}
