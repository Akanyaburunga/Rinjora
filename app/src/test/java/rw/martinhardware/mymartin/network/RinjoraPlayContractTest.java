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

import rw.martinhardware.mymartin.network.dto.AnswerResponseDto;
import rw.martinhardware.mymartin.network.dto.HintDto;
import rw.martinhardware.mymartin.network.dto.HistoryEntryDto;
import rw.martinhardware.mymartin.network.dto.HistoryStatsDto;
import rw.martinhardware.mymartin.network.dto.RevealDto;

/**
 * Verifies the core game-loop DTOs (plan §2.8–§2.11) parse correctly against the
 * backend contract examples.
 */
public class RinjoraPlayContractTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesHintEnvelope() {
        String json = "{\"success\":true,\"data\":"
                + "{\"id\":5,\"hint\":\"A map.\",\"hint2\":\"It has no houses.\",\"hints_revealed\":2}}";
        Type type = new TypeToken<ApiEnvelope<HintDto>>() {}.getType();
        ApiEnvelope<HintDto> envelope = gson.fromJson(json, type);
        assertTrue(envelope.isSuccess());
        assertEquals(5L, envelope.getData().getId());
        assertEquals("A map.", envelope.getData().getHint());
        assertEquals("It has no houses.", envelope.getData().getHint2());
        assertEquals(2, envelope.getData().getHintsRevealed());
    }

    @Test
    public void parsesAnswerEnvelope() {
        String json = "{\"success\":true,\"data\":{"
                + "\"correct\":true,\"rewarded\":true,\"points\":5,\"capped\":false,"
                + "\"message\":\"Correct! You earned 5 reputation points.\","
                + "\"new_achievements\":["
                + "  {\"slug\":\"first_riddle\",\"name\":\"First Riddle\","
                + "   \"description\":\"Solve your first riddle\",\"icon\":\"https://x/icon.png\"}"
                + "]}}";
        Type type = new TypeToken<ApiEnvelope<AnswerResponseDto>>() {}.getType();
        ApiEnvelope<AnswerResponseDto> envelope = gson.fromJson(json, type);
        AnswerResponseDto a = envelope.getData();
        assertTrue(a.isCorrect());
        assertTrue(a.isRewarded());
        assertEquals(5, a.getPoints());
        assertFalse(a.isCapped());
        assertEquals("Correct! You earned 5 reputation points.", a.getMessage());
        assertEquals(1, a.getNewAchievements().size());
        assertEquals("first_riddle", a.getNewAchievements().get(0).getSlug());
        assertEquals("First Riddle", a.getNewAchievements().get(0).getName());
    }

    @Test
    public void parsesFailedAnswerEnvelope() {
        String json = "{\"success\":true,\"data\":"
                + "{\"correct\":false,\"rewarded\":false,\"points\":0,\"capped\":false,"
                + "\"message\":\"Not quite. Try again.\",\"new_achievements\":[]}}";
        Type type = new TypeToken<ApiEnvelope<AnswerResponseDto>>() {}.getType();
        ApiEnvelope<AnswerResponseDto> envelope = gson.fromJson(json, type);
        AnswerResponseDto a = envelope.getData();
        assertFalse(a.isCorrect());
        assertFalse(a.isRewarded());
        assertEquals(0, a.getPoints());
        assertTrue(a.getNewAchievements().isEmpty());
    }

    @Test
    public void parsesRevealEnvelope() {
        String json = "{\"success\":true,\"data\":"
                + "{\"id\":5,\"question\":\"I have cities but no houses…\",\"answer\":\"a map\"}}";
        Type type = new TypeToken<ApiEnvelope<RevealDto>>() {}.getType();
        ApiEnvelope<RevealDto> envelope = gson.fromJson(json, type);
        assertEquals(5L, envelope.getData().getId());
        assertEquals("a map", envelope.getData().getAnswer());
    }

    @Test
    public void parsesHistoryStatsEnvelope() {
        String json = "{\"success\":true,\"data\":{"
                + "\"total_attempts\":12,\"riddles_solved\":8,\"unique_riddles\":8,\"accuracy\":66.7,"
                + "\"by_category\":[{\"category_id\":2,\"name\":\"Inkuru\",\"attempts\":5,\"solved\":4}]}}";
        Type type = new TypeToken<ApiEnvelope<HistoryStatsDto>>() {}.getType();
        ApiEnvelope<HistoryStatsDto> envelope = gson.fromJson(json, type);
        HistoryStatsDto s = envelope.getData();
        assertEquals(12, s.getTotalAttempts());
        assertEquals(8, s.getRiddlesSolved());
        assertEquals(8, s.getUniqueRiddles());
        assertEquals(66.7, s.getAccuracy(), 0.001);
        assertEquals(1, s.getByCategory().size());
        assertEquals("Inkuru", s.getByCategory().get(0).getName());
        assertEquals(4, s.getByCategory().get(0).getSolved());
    }

    @Test
    public void parsesHistoryEntryEnvelope() {
        String json = "{\"success\":true,\"data\":[{"
                + "\"id\":11,"
                + "\"riddle\":{\"id\":5,\"solved\":false,\"hints_revealed\":0,"
                + "  \"category\":{\"id\":2,\"name\":\"Inkuru\",\"slug\":\"inkuru\"},"
                + "  \"question\":\"I have cities but no houses…\",\"difficulty\":\"medium\","
                + "  \"riddle_type\":\"riddle\",\"tags\":[],\"hint\":\"A map.\",\"hint2\":null,"
                + "  \"created_at\":\"2026-01-01T00:00:00Z\"},"
                + "\"submitted_answer\":\"a map\",\"is_correct\":true,\"rewarded\":true,"
                + "\"attempted_at\":\"2026-01-01T00:00:00Z\"}]}";
        Type type = new TypeToken<ApiEnvelope<List<HistoryEntryDto>>>() {}.getType();
        ApiEnvelope<List<HistoryEntryDto>> envelope = gson.fromJson(json, type);
        HistoryEntryDto e = envelope.getData().get(0);
        assertNotNull(e.getRiddle());
        assertEquals("I have cities but no houses…", e.getRiddle().getQuestion());
        assertEquals("a map", e.getSubmittedAnswer());
        assertTrue(e.isCorrect());
        assertTrue(e.isRewarded());
    }
}
