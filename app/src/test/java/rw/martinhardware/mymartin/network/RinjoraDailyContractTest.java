package rw.martinhardware.mymartin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;

import rw.martinhardware.mymartin.network.dto.DailyRiddleDto;
import rw.martinhardware.mymartin.network.dto.DailyStatusDto;
import rw.martinhardware.mymartin.network.dto.FreezeResponseDto;

/**
 * Verifies the daily-riddle & streak DTOs (plan §2.4–§2.6, §2.12) parse against
 * the backend contract examples.
 */
public class RinjoraDailyContractTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesDailyRiddleEnvelope() {
        String json = "{\"success\":true,\"data\":{"
                + "\"streak\":{\"current\":2,\"longest\":5},"
                + "\"solved_by_count\":128,\"best_streak\":21,"
                + "\"daily\":{\"id\":3,\"solved\":false,\"hints_revealed\":0,"
                + "  \"category\":{\"id\":2,\"name\":\"Inkuru\",\"slug\":\"inkuru\"},"
                + "  \"question\":\"I have cities but no houses…\",\"difficulty\":\"easy\","
                + "  \"riddle_type\":\"riddle\",\"tags\":[],\"hint\":\"A map.\",\"hint2\":null,"
                + "  \"created_at\":\"2026-01-01T00:00:00Z\"}}}";
        Type type = new TypeToken<ApiEnvelope<DailyRiddleDto>>() {}.getType();
        ApiEnvelope<DailyRiddleDto> envelope = gson.fromJson(json, type);
        assertTrue(envelope.isSuccess());
        DailyRiddleDto d = envelope.getData();
        assertEquals(2, d.getStreak().getCurrent());
        assertEquals(5, d.getStreak().getLongest());
        assertEquals(128, d.getSolvedByCount());
        assertEquals(21, d.getBestStreak());
        assertEquals(3L, d.getDaily().getId());
        assertFalse(d.getDaily().isSolved());
        assertEquals("I have cities but no houses…", d.getDaily().getQuestion());
    }

    @Test
    public void parsesDailyStatusEnvelope() {
        String json = "{\"success\":true,\"data\":{"
                + "\"daily_available\":true,\"streak_at_risk\":false,"
                + "\"pending_challenges\":1,"
                + "\"streak\":{\"current\":2,\"longest\":5}}}";
        Type type = new TypeToken<ApiEnvelope<DailyStatusDto>>() {}.getType();
        ApiEnvelope<DailyStatusDto> envelope = gson.fromJson(json, type);
        DailyStatusDto s = envelope.getData();
        assertTrue(s.isDailyAvailable());
        assertFalse(s.isStreakAtRisk());
        assertEquals(1, s.getPendingChallenges());
        assertEquals(2, s.getStreak().getCurrent());
    }

    @Test
    public void parsesFreezeResponseEnvelope() {
        String json = "{\"success\":true,\"data\":{"
                + "\"freezes_remaining\":2,\"freeze_active\":true,"
                + "\"streak\":{\"current\":2,\"longest\":5}}}";
        Type type = new TypeToken<ApiEnvelope<FreezeResponseDto>>() {}.getType();
        ApiEnvelope<FreezeResponseDto> envelope = gson.fromJson(json, type);
        FreezeResponseDto f = envelope.getData();
        assertEquals(2, f.getFreezesRemaining());
        assertTrue(f.isFreezeActive());
        assertEquals(2, f.getStreak().getCurrent());
    }
}
