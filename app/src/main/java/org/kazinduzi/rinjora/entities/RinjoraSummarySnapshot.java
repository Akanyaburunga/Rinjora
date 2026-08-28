package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

/**
 * Offline-first cache for the Rinjora Home screen ({@code GET /me/summary},
 * plan §4.1). One row per user, overwritten on every sync. The UI reads this
 * first so Home renders instantly even fully offline, then refreshes in the
 * background.
 */
@Entity
public class RinjoraSummarySnapshot {

    @Id
    public long id;

    @Index
    public long userId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    // --- user ---
    public String name;
    public String profilePictureUrl;

    // --- points / level ---
    public int reputation;
    public int level;

    // --- streak ---
    public int currentStreak;
    public int longestStreak;

    // --- badges ---
    public int earnedBadges;
    public int totalBadges;

    // --- favorites ---
    public int favoritesCount;

    // --- activity ---
    public int totalAttempts;
    public int riddlesSolved;
    public double accuracy;
    public int uniqueRiddles;
    public int submissionsCount;
    public int sharesCount;

    /** Raw JSON payload, kept for future screens / debugging. */
    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getEarnedBadges() { return earnedBadges; }
    public void setEarnedBadges(int earnedBadges) { this.earnedBadges = earnedBadges; }
    public int getTotalBadges() { return totalBadges; }
    public void setTotalBadges(int totalBadges) { this.totalBadges = totalBadges; }

    public int getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(int favoritesCount) { this.favoritesCount = favoritesCount; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
    public int getRiddlesSolved() { return riddlesSolved; }
    public void setRiddlesSolved(int riddlesSolved) { this.riddlesSolved = riddlesSolved; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public int getUniqueRiddles() { return uniqueRiddles; }
    public void setUniqueRiddles(int uniqueRiddles) { this.uniqueRiddles = uniqueRiddles; }
    public int getSubmissionsCount() { return submissionsCount; }
    public void setSubmissionsCount(int submissionsCount) { this.submissionsCount = submissionsCount; }
    public int getSharesCount() { return sharesCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
