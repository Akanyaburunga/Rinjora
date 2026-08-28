package org.kazinduzi.rinjora.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import org.kazinduzi.rinjora.network.dto.DuelDto;
import org.kazinduzi.rinjora.network.dto.DuelSolveResponseDto;

/**
 * Verifies Phase I DTOs (plan §7): {@code DuelDto} from the duels list/§7.1 and
 * §7.3, and {@code DuelSolveResponseDto} from §7.6 — including the anti-cheat
 * invariant (opponent answer never present).
 */
public class RinjoraDuelContractTest {

    private final Gson gson = new Gson();

    private static class Envelope<T> {
        boolean success;
        T data;
    }

    @Test
    public void parsesDuelList() {
        String json = "{\"success\":true,\"data\":["
                + "  {\"id\":11,\"status\":\"pending\",\"wager\":10,"
                + "   \"direction\":\"incoming\","
                + "   \"accepted_at\":null,\"resolved_at\":null,"
                + "   \"riddle\":{\"id\":3,\"question\":\"What runs but never walks?\","
                + "              \"difficulty\":\"easy\",\"riddle_type\":\"riddle\","
                + "              \"answer\":null,"
                + "              \"category\":{\"id\":1,\"name\":\"General\",\"slug\":\"general\"}},"
                + "   \"initiator\":{\"id\":2,\"name\":\"Aline\",\"reputation\":300},"
                + "   \"opponent\":{\"id\":1,\"name\":\"Blaise\",\"reputation\":120},"
                + "   \"my_attempt\":null,\"opponent_attempt\":null,"
                + "   \"winner_id\":null,\"created_at\":\"2026-08-28T10:00:00Z\"}"
                + "]}";

        Type type = new TypeToken<Envelope<List<DuelDto>>>() {}.getType();
        Envelope<List<DuelDto>> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertEquals(1, env.data.size());

        DuelDto duel = env.data.get(0);
        assertEquals(11, duel.getId());
        assertEquals("pending", duel.getStatus());
        assertEquals(10, duel.getWager());
        assertEquals("incoming", duel.getDirection());
        assertEquals(false, duel.isOutgoing());

        assertEquals("Aline", duel.getInitiator().getName());
        assertEquals(300, duel.getInitiator().getReputation());
        assertEquals("Blaise", duel.getOpponent().getName());
        assertEquals(120, duel.getOpponent().getReputation());
        assertNull(duel.getWinnerId());

        assertTrue(duel.getRiddle() != null);
        assertEquals(3, duel.getRiddle().getId());
        assertEquals("What runs but never walks?", duel.getRiddle().getQuestion());
    }

    @Test
    public void parsesSolveResponse() {
        String json = "{\"success\":true,\"data\":{"
                + "\"correct\":true,\"resolved\":false,\"answer\":null,"
                + "\"message\":\"Correct! Waiting on your opponent.\"}}";

        Type type = new TypeToken<Envelope<DuelSolveResponseDto>>() {}.getType();
        Envelope<DuelSolveResponseDto> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertSame(Boolean.TRUE, env.data.getCorrect());
        assertEquals(false, env.data.isResolved());
        assertNull(env.data.getAnswer());
        assertEquals("Correct! Waiting on your opponent.", env.data.getMessage());
    }
}
