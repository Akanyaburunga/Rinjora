package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for a single proverb while it is being played in
 * Heraheza mode ({@code GET /proverbs/{id}}, parity plan §2.2). The play screen
 * reads this first so it survives rotation and short offline gaps.
 * <p>
 * The {@code answer} is intentionally <b>never</b> persisted: it is confidential
 * and only ever shown after a correct solve or an explicit reveal.
 */
@Entity
public class RinjoraProverbSnapshot {

    @Id
    public long id;

    /** The proverb's backend id. */
    public long proverbId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    public long categoryId;
    public String categoryName;

    public String question;
    public String difficulty;
    public String source;

    public boolean solved;

    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProverbId() { return proverbId; }
    public void setProverbId(long proverbId) { this.proverbId = proverbId; }
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
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
