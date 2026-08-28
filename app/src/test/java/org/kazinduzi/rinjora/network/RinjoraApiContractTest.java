package org.kazinduzi.rinjora.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import org.kazinduzi.rinjora.network.dto.RiddleDto;

/**
 * Verifies the {@code { success, data }} envelope and riddle DTO parsing against
 * the example payloads in docs/android-app-implementation-plan.md (§0, §2).
 */
public class RinjoraApiContractTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesRiddleListEnvelope() {
        String json = "{"
                + "\"success\": true,"
                + "\"data\": ["
                + "  {"
                + "    \"id\": 5, \"solved\": false, \"hints_revealed\": 0,"
                + "    \"category\": { \"id\": 2, \"name\": \"Inkuru\", \"slug\": \"inkuru\" },"
                + "    \"question\": \"I have cities but no houses...\", \"difficulty\": \"medium\","
                + "    \"riddle_type\": \"riddle\", \"tags\": [],"
                + "    \"hint\": \"A map.\", \"hint2\": null,"
                + "    \"created_at\": \"2026-08-28T10:00:00Z\""
                + "  }"
                + "]"
                + "}";

        Type type = new TypeToken<ApiEnvelope<List<RiddleDto>>>() {}.getType();
        ApiEnvelope<List<RiddleDto>> envelope = gson.fromJson(json, type);

        assertTrue(envelope.isSuccess());
        assertNotNull(envelope.getData());
        assertEquals(1, envelope.getData().size());

        RiddleDto riddle = envelope.getData().get(0);
        assertEquals(5L, riddle.getId());
        assertFalse(riddle.isSolved());
        assertEquals(0, riddle.getHintsRevealed());
        assertEquals("Inkuru", riddle.getCategory().getName());
        assertEquals("I have cities but no houses...", riddle.getQuestion());
        assertEquals("medium", riddle.getDifficulty());
        assertEquals("riddle", riddle.getRiddleType());
        assertEquals("A map.", riddle.getHint());
    }

    @Test
    public void riddleDtoNeverBindsAnswerFromPayload() {
        // The plan (§2) says the backend deliberately omits "answer"; even if a
        // rogue payload contained one, RiddleDto must not expose it.
        String json = "{ \"success\": true, \"data\": { \"id\": 3, \"question\": \"Q\", \"answer\": \"SECRET\" } }";

        Type type = new TypeToken<ApiEnvelope<RiddleDto>>() {}.getType();
        ApiEnvelope<RiddleDto> envelope = gson.fromJson(json, type);

        // RiddleDto has no answer field by design - it simply won't be exposed.
        assertTrue(envelope.isSuccess());
        assertEquals(3L, envelope.getData().getId());
        // (The existence of a public getAnswer() would be a security regression.)
    }

    @Test
    public void treatsErrorEnvelopeEvenOnHttp200StyleShape() {
        String json = "{ \"success\": false, \"message\": \"Not quite. Try again.\", \"data\": null }";

        Type type = new TypeToken<ApiEnvelope<Object>>() {}.getType();
        ApiEnvelope<Object> envelope = gson.fromJson(json, type);

        assertFalse(envelope.isSuccess());
        assertEquals("Not quite. Try again.", envelope.getMessage());
        assertNull(envelope.getData());
    }
}
