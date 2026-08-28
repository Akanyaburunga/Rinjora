package rw.martinhardware.mymartin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import rw.martinhardware.mymartin.network.dto.LeaderboardEnvelope;

/**
 * Verifies the leaderboard DTOs (plan §5.1) parse against the example payload,
 * including the custom envelope's top-level {@code filter}/{@code me}/{@code meta}.
 */
public class RinjoraLeaderboardContractTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesLeaderboardEnvelope() {
        String json = "{\"success\":true,\"filter\":\"all_time\",\"data\":["
                + "  {\"rank\":1,\"id\":2,\"name\":\"Aline\",\"points\":450,"
                + "   \"words_contributed\":3,\"meanings_contributed\":2,"
                + "   \"profile_picture_url\":\"https://x/a.png\"}"
                + "],"
                + "\"me\":{\"id\":1,\"name\":\"Blaise\",\"rank\":12,\"points\":120,"
                + "       \"total_players\":40,\"percentile\":72},"
                + "\"meta\":{\"current_page\":1,\"per_page\":20,\"total\":40,\"last_page\":2}}";

        LeaderboardEnvelope e = gson.fromJson(json, LeaderboardEnvelope.class);

        assertTrue(e.isSuccess());
        assertEquals("all_time", e.getFilter());
        assertEquals(1, e.getData().size());
        assertEquals(1, e.getData().get(0).getRank());
        assertEquals("Aline", e.getData().get(0).getName());
        assertEquals(450, e.getData().get(0).getPoints());
        assertEquals(3, e.getData().get(0).getWordsContributed());
        assertEquals(2, e.getData().get(0).getMeaningsContributed());

        assertNotNull(e.getMe());
        assertEquals("Blaise", e.getMe().getName());
        assertEquals(12, e.getMe().getRank());
        assertEquals(40, e.getMe().getTotalPlayers());
        assertEquals(72.0, e.getMe().getPercentile(), 0.001);

        assertNotNull(e.getMeta());
        assertEquals(1, e.getMeta().getCurrentPage());
        assertEquals(20, e.getMeta().getPerPage());
        assertEquals(40, e.getMeta().getTotal());
        assertEquals(2, e.getMeta().getLastPage());
    }
}
