package rw.martinhardware.mymartin.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for a single Riddle while it is being played
 * ({@code GET /riddles/{id}}, plan §2.2). The play screen reads this first so
 * it survives rotation and short offline gaps, then refreshes in the background.
 * <p>
 * The {@code answer} is intentionally <b>never</b> persisted: it is confidential
 * and only ever shown after a correct solve or an explicit reveal.
 */
@Entity
public class RinjoraRiddleSnapshot {

    @Id
    public long id;

    /** The riddle's backend id. */
    public long riddleId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    public long categoryId;
    public String categoryName;

    public String question;
    public String difficulty;
    public String riddleType;

    /** How many hints have been revealed so far (server authoritative). */
    public int hintsRevealed;

    public boolean solved;

    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getRiddleId() { return riddleId; }
    public void setRiddleId(long riddleId) { this.riddleId = riddleId; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getRiddleType() { return riddleType; }
    public void setRiddleType(String riddleType) { this.riddleType = riddleType; }
    public int getHintsRevealed() { return hintsRevealed; }
    public void setHintsRevealed(int hintsRevealed) { this.hintsRevealed = hintsRevealed; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
