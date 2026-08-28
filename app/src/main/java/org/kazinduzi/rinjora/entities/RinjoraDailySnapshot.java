package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for the Daily riddle screen (plan §2.4/§2.6). Holds the
 * latest daily status plus a lightweight copy of today's daily riddle so the
 * screen renders instantly on opening, then refreshes in the background.
 * <p>
 * Daily is deterministic per user/date — it is <b>not</b> treated as a single
 * "today" across dates; the cache is only a render aid, and the server stays
 * authoritative for clocks and availability.
 */
@Entity
public class RinjoraDailySnapshot {

    @Id
    public long id;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    // --- status (plan §2.6) ---
    public boolean dailyAvailable;
    public boolean streakAtRisk;
    public int pendingChallenges;

    // --- streak ---
    public int currentStreak;
    public int longestStreak;

    // --- daily riddle (lightweight) ---
    public long dailyRiddleId;
    public String dailyQuestion;
    public boolean dailySolved;

    /** Raw JSON payload (e.g. daily status), kept for future screens / debugging. */
    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }

    public boolean isDailyAvailable() { return dailyAvailable; }
    public void setDailyAvailable(boolean dailyAvailable) { this.dailyAvailable = dailyAvailable; }
    public boolean isStreakAtRisk() { return streakAtRisk; }
    public void setStreakAtRisk(boolean streakAtRisk) { this.streakAtRisk = streakAtRisk; }
    public int getPendingChallenges() { return pendingChallenges; }
    public void setPendingChallenges(int pendingChallenges) { this.pendingChallenges = pendingChallenges; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public long getDailyRiddleId() { return dailyRiddleId; }
    public void setDailyRiddleId(long dailyRiddleId) { this.dailyRiddleId = dailyRiddleId; }
    public String getDailyQuestion() { return dailyQuestion; }
    public void setDailyQuestion(String dailyQuestion) { this.dailyQuestion = dailyQuestion; }
    public boolean isDailySolved() { return dailySolved; }
    public void setDailySolved(boolean dailySolved) { this.dailySolved = dailySolved; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
