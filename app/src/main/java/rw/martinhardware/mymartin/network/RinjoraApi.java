package rw.martinhardware.mymartin.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import rw.martinhardware.mymartin.network.dto.AchievementLibraryDto;
import rw.martinhardware.mymartin.network.dto.AnswerResponseDto;
import rw.martinhardware.mymartin.network.dto.CategoryDto;
import rw.martinhardware.mymartin.network.dto.DailyRiddleDto;
import rw.martinhardware.mymartin.network.dto.DailyStatusDto;
import rw.martinhardware.mymartin.network.dto.FreezeResponseDto;
import rw.martinhardware.mymartin.network.dto.HintDto;
import rw.martinhardware.mymartin.network.dto.HistoryEntryDto;
import rw.martinhardware.mymartin.network.dto.HistoryStatsDto;
import rw.martinhardware.mymartin.network.dto.LeaderboardEnvelope;
import rw.martinhardware.mymartin.network.dto.LoginResponseDto;
import rw.martinhardware.mymartin.network.dto.RevealDto;
import rw.martinhardware.mymartin.network.dto.RiddleDto;
import rw.martinhardware.mymartin.network.dto.ShareDto;
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

    // ------------------------------------------------------------------
    // Play loop (plan §2.8–§2.11)
    // ------------------------------------------------------------------

    @GET("riddles/{id}/hint")
    Call<ApiEnvelope<HintDto>> hint(@Path("id") long id);

    @POST("riddles/{id}/answer")
    Call<ApiEnvelope<AnswerResponseDto>> answer(@Path("id") long id, @Body Map<String, Object> body);

    @POST("riddles/{id}/reveal")
    Call<ApiEnvelope<RevealDto>> reveal(@Path("id") long id);

    @GET("riddles/history")
    Call<ApiEnvelope<List<HistoryEntryDto>>> history(@Query("per_page") int perPage);

    @GET("riddles/history/stats")
    Call<ApiEnvelope<HistoryStatsDto>> historyStats();

    // ------------------------------------------------------------------
    // Favorites & sharing (plan §6.1–§6.2)
    // ------------------------------------------------------------------

    @GET("me/favorites")
    Call<ApiEnvelope<List<RiddleDto>>> favorites();

    @POST("me/favorites/{riddle}")
    Call<ApiEnvelope<Void>> addFavorite(@Path("riddle") long riddleId);

    @DELETE("me/favorites/{riddle}")
    Call<ApiEnvelope<Void>> removeFavorite(@Path("riddle") long riddleId);

    @POST("riddles/{id}/share")
    Call<ApiEnvelope<ShareDto>> share(@Path("id") long id, @Body Map<String, Object> body);

    // ------------------------------------------------------------------
    // Achievements / badges (plan §4.4)
    // ------------------------------------------------------------------

    @GET("me/achievements")
    Call<ApiEnvelope<AchievementLibraryDto>> achievements();

    // ------------------------------------------------------------------
    // Daily riddle & streak (plan §2.4–§2.6, §2.12)
    // ------------------------------------------------------------------

    @GET("riddles/daily")
    Call<ApiEnvelope<DailyRiddleDto>> dailyRiddle();

    @GET("riddles/daily/history")
    Call<ApiEnvelope<DailyRiddleDto>> dailyHistory(@Query("date") String date);

    @GET("riddles/daily/status")
    Call<ApiEnvelope<DailyStatusDto>> dailyStatus();

    @POST("riddles/streak/freeze")
    Call<ApiEnvelope<FreezeResponseDto>> freezeStreak();

    // ------------------------------------------------------------------
    // Leaderboard (plan §5.1)
    // ------------------------------------------------------------------

    // Returns the custom LeaderboardEnvelope (has top-level `me`/`meta`/`filter`,
    // so it does not fit the generic ApiEnvelope wrapper).
    @GET("leaderboard")
    Call<LeaderboardEnvelope> leaderboard(@Query("filter") String filter,
                                          @Query("page") int page,
                                          @Query("per_page") int perPage);
}
