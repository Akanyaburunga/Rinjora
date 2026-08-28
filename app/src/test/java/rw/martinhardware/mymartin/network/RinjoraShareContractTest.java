package rw.martinhardware.mymartin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import rw.martinhardware.mymartin.network.dto.RiddleDto;
import rw.martinhardware.mymartin.network.dto.ShareDto;

/**
 * Verifies Phase G DTOs (plan §6): {@code ShareDto} from
 * {@code POST /riddles/{id}/share}, and the favorites list from
 * {@code GET /me/favorites} (solved-marked riddle payloads).
 */
public class RinjoraShareContractTest {

    private final Gson gson = new Gson();

    private static class Envelope<T> {
        boolean success;
        T data;
    }

    @Test
    public void parsesShareEnvelope() {
        String json = "{\"success\":true,"
                + "\"data\":{\"share_url\":\"https://kazinduzi.bi/r/k7f2xa\","
                + "          \"code\":\"k7f2xa\"}}";

        Type type = new TypeToken<Envelope<ShareDto>>() {}.getType();
        Envelope<ShareDto> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertNotNull(env.data);
        assertEquals("https://kazinduzi.bi/r/k7f2xa", env.data.getShareUrl());
        assertEquals("k7f2xa", env.data.getCode());
    }

    @Test
    public void parsesFavoritesList() {
        String json = "{\"success\":true,\"data\":["
                + "  {\"id\":9,\"question\":\"What has a key but opens no lock?\","
                + "   \"difficulty\":\"easy\",\"riddle_type\":\"classic\","
                + "   \"solved\":true,\"hints_revealed\":1,"
                + "   \"category\":{\"id\":1,\"name\":\"General\",\"slug\":\"general\"}},"
                + "  {\"id\":10,\"question\":\"I speak without a mouth…\","
                + "   \"difficulty\":\"medium\",\"riddle_type\":\"classic\","
                + "   \"solved\":false,\"hints_revealed\":0,"
                + "   \"category\":{\"id\":2,\"name\":\"Animals\",\"slug\":\"animals\"}}"
                + "]}";

        Type type = new TypeToken<Envelope<List<RiddleDto>>>() {}.getType();
        Envelope<List<RiddleDto>> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertNotNull(env.data);
        assertEquals(2, env.data.size());

        RiddleDto first = env.data.get(0);
        assertEquals(9, first.getId());
        assertEquals("What has a key but opens no lock?", first.getQuestion());
        assertTrue(first.isSolved());
        assertEquals(1, first.getHintsRevealed());
        assertNotNull(first.getCategory());
        assertEquals("General", first.getCategory().getName());

        RiddleDto second = env.data.get(1);
        assertFalse(second.isSolved());
        assertEquals("Animals", second.getCategory().getName());
    }
}
