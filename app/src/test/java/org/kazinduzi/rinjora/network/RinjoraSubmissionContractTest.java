package org.kazinduzi.rinjora.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import org.kazinduzi.rinjora.network.dto.SubmissionDto;

/**
 * Verifies Phase J DTOs (plan §8): {@code SubmissionDto} from both the
 * {@code POST /submissions/riddles} response and the {@code GET /submissions/riddles}
 * list (status + {@code rejection_reason}).
 */
public class RinjoraSubmissionContractTest {

    private final Gson gson = new Gson();

    private static class Envelope<T> {
        boolean success;
        T data;
    }

    @Test
    public void parsesSubmissionList() {
        String json = "{\"success\":true,\"data\":["
                + "  {\"id\":1,\"question\":\"What am I?\",\"status\":\"pending\","
                + "   \"difficulty\":\"easy\",\"riddle_type\":\"classic\","
                + "   \"hint\":\"Think of water\",\"source\":\"original\","
                + "   \"rejection_reason\":null,"
                + "   \"created_at\":\"2026-08-28T10:00:00Z\"},"
                + "  {\"id\":2,\"question\":\"A rejected one\",\"status\":\"rejected\","
                + "   \"difficulty\":\"hard\",\"riddle_type\":\"words\","
                + "   \"rejection_reason\":\"Duplicate answer already exists.\","
                + "   \"created_at\":\"2026-08-27T09:00:00Z\"}"
                + "]}";

        Type type = new TypeToken<Envelope<List<SubmissionDto>>>() {}.getType();
        Envelope<List<SubmissionDto>> env = gson.fromJson(json, type);

        assertTrue(env.success);
        assertEquals(2, env.data.size());

        SubmissionDto pending = env.data.get(0);
        assertEquals(1, pending.getId());
        assertEquals("What am I?", pending.getQuestion());
        assertEquals("pending", pending.getStatus());
        assertEquals("easy", pending.getDifficulty());
        assertEquals("classic", pending.getRiddleType());
        assertEquals("Think of water", pending.getHint());
        assertEquals("original", pending.getSource());
        assertNull(pending.getRejectionReason());

        SubmissionDto rejected = env.data.get(1);
        assertEquals("rejected", rejected.getStatus());
        assertEquals("Duplicate answer already exists.", rejected.getRejectionReason());
    }
}
