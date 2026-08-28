package rw.martinhardware.mymartin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;

import rw.martinhardware.mymartin.network.dto.SummaryDto;

/**
 * Verifies {@code GET /me/summary} DTO parsing against the example payload in
 * docs/android-app-implementation-plan.md (§4.1).
 */
public class RinjoraSummaryContractTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesSummaryEnvelope() {
        String json = "{"
                + "\"success\": true,"
                + "\"data\": {"
                + "  \"user\": { \"id\": 1, \"name\": \"Blaise\", \"profile_picture_url\": \"https://x/default.png\" },"
                + "  \"points\": { \"reputation\": 120, \"level\": 3 },"
                + "  \"streak\": { \"current\": 4, \"longest\": 9 },"
                + "  \"badges\": { \"earned_count\": 5, \"total\": 10, \"earned_slugs\": [\"first_riddle\",\"streak_3\"] },"
                + "  \"favorites_count\": 7,"
                + "  \"activity\": { \"total_attempts\": 40, \"riddles_solved\": 25, \"accuracy\": 62.5,"
                + "                     \"unique_riddles\": 24, \"submissions_count\": 2, \"shares_count\": 3 }"
                + "}"
                + "}";

        Type type = new TypeToken<ApiEnvelope<SummaryDto>>() {}.getType();
        ApiEnvelope<SummaryDto> envelope = gson.fromJson(json, type);

        assertTrue(envelope.isSuccess());
        SummaryDto s = envelope.getData();
        assertNotNull(s);

        assertEquals(1L, s.getUser().getId());
        assertEquals("Blaise", s.getUser().getName());

        assertEquals(120, s.getPoints().getReputation());
        assertEquals(3, s.getPoints().getLevel());

        assertEquals(4, s.getStreak().getCurrent());
        assertEquals(9, s.getStreak().getLongest());

        assertEquals(5, s.getBadges().getEarnedCount());
        assertEquals(10, s.getBadges().getTotal());
        assertEquals(2, s.getBadges().getEarnedSlugs().size());

        assertEquals(7, s.getFavoritesCount());

        assertEquals(40, s.getActivity().getTotalAttempts());
        assertEquals(25, s.getActivity().getRiddlesSolved());
        assertEquals(62.5, s.getActivity().getAccuracy(), 0.001);
        assertEquals(24, s.getActivity().getUniqueRiddles());
        assertEquals(2, s.getActivity().getSubmissionsCount());
        assertEquals(3, s.getActivity().getSharesCount());
    }
}
