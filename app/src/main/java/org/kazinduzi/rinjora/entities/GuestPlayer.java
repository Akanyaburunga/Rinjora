package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Aggregate progress for a guest (unsigned-in) player, stored locally in ObjectBox
 * so the app is fully playable without an account (plan: "play first, create an
 * account later, then sync").
 *
 * <p>There is at most one active guest row. Once the user registers/logs in, the
 * aggregate is uploaded to the server and this row is cleared; {@link GuestProgress}
 * carries the finer-grained, syncable records.
 */
@Entity
public class GuestPlayer {

    @Id
    public long id;

    /** Total points earned offline. */
    public long totalPoints;

    /** Number of riddles solved offline. */
    public long riddlesSolved;

    /** Current streak count. */
    public long currentStreak;

    /** Best streak ever reached offline. */
    public long bestStreak;

    /** ms epoch of the last offline play. */
    public long lastPlayedAt;

    /** True once the guest has created an account and uploaded their progress. */
    public boolean syncedToAccount;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTotalPoints() { return totalPoints; }
    public void setTotalPoints(long totalPoints) { this.totalPoints = totalPoints; }
    public long getRiddlesSolved() { return riddlesSolved; }
    public void setRiddlesSolved(long riddlesSolved) { this.riddlesSolved = riddlesSolved; }
    public long getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(long currentStreak) { this.currentStreak = currentStreak; }
    public long getBestStreak() { return bestStreak; }
    public void setBestStreak(long bestStreak) { this.bestStreak = bestStreak; }
    public long getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(long lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
    public boolean isSyncedToAccount() { return syncedToAccount; }
    public void setSyncedToAccount(boolean syncedToAccount) { this.syncedToAccount = syncedToAccount; }
}
