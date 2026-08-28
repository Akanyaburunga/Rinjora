package rw.martinhardware.mymartin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;

import rw.martinhardware.mymartin.network.dto.AchievementLibraryDto;

/**
 * Verifies Phase H DTOs (plan §4.4): {@code GET /me/achievements} parse against
 * the example payload — earned count/total plus the badge library with progress.
 */
public class RinjoraAchievementsContractTest {

    private final Gson gson = new Gson();

    private static class Envelope<T> {
        boolean success;
        T data;
    }

    @Test
    public void parsesAchievementLibrary() {
        String json = "{\"success\":true,\"data\":{"
                + "\"earned_count\":2,\"total\":3,"
                + "\"achievements\":["
                + "  {\"id\":1,\"slug\":\"streak_3\",\"name\":\"3-Day Streak\","
                + "   \"description\":\"Keep a 3-day streak\",\"category\":\"streak\","
                + "   \"icon\":\"streak3\",\"threshold\":3,\"metric\":\"current_streak\","
                + "   \"earned\":true,\"earned_at\":\"2026-08-01T00:00:00Z\","
                + "   \"progress\":3,\"goal\":3},"
                + "  {\"id\":2,\"slug\":\"streak_7\",\"name\":\"7-Day Streak\","
                + "   \"description\":\"Keep a 7-day streak\",\"category\":\"streak\","
                + "   \"icon\":\"streak7\",\"threshold\":7,\"metric\":\"current_streak\","
                + "   \"earned\":false,\"earned_at\":null,"
                + "   \"progress\":3,\"goal\":7},"
                + "  {\"id\":3,\"slug\":\"solver_10\",\"name\":\"Riddle Master\","
                + "   \"description\":\"Solve 10 riddles\",\"category\":\"riddle\","
                + "   \"icon\":\"solver\",\"threshold\":10,\"metric\":\"riddles_solved\","
                + "   \"earned\":true,\"earned_at\":\"2026-08-15T00:00:00Z\","
                + "   \"progress\":12,\"goal\":10}"
                + "]}}";

        Type type = new TypeToken<Envelope<AchievementLibraryDto>>() {}.getType();
        Envelope<AchievementLibraryDto> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertNotNull(env.data);
        assertEquals(2, env.data.getEarnedCount());
        assertEquals(3, env.data.getTotal());
        assertEquals(3, env.data.getAchievements().size());

        rw.martinhardware.mymartin.network.dto.BadgeDto earned =
                env.data.getAchievements().get(0);
        assertEquals(1, earned.getId());
        assertEquals("streak_3", earned.getSlug());
        assertEquals("3-Day Streak", earned.getName());
        assertEquals("streak", earned.getCategory());
        assertEquals(3, earned.getThreshold());
        assertEquals("current_streak", earned.getMetric());
        assertTrue(earned.isEarned());
        assertEquals(3, earned.getProgress());
        assertEquals(3, earned.getGoal());

        rw.martinhardware.mymartin.network.dto.BadgeDto locked =
                env.data.getAchievements().get(1);
        assertFalse(locked.isEarned());
        assertEquals(3, locked.getProgress());
        assertEquals(7, locked.getGoal());
    }
}
