package rw.martinhardware.mymartin.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for the last-viewed leaderboard (plan §5.1). Holds the
 * period/filter, page metadata, and the full leaderboard envelope JSON so the
 * board can be re-rendered offline. Kept as one row per filter (latest page).
 */
@Entity
public class RinjoraLeaderboardSnapshot {

    @Id
    public long id;

    /** The filter period (today|this_week|this_month|this_year|all_time). */
    public String filter;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    public int currentPage;
    public int lastPage;
    public int total;

    // --- me (rendered as a highlight row) ---
    public boolean hasMe;
    public String meName;
    public int meRank;
    public int mePoints;
    public int meTotalPlayers;
    public double mePercentile;

    /** Raw {@code LeaderboardEnvelope} JSON, re-parsed for offline rendering. */
    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public boolean isHasMe() { return hasMe; }
    public void setHasMe(boolean hasMe) { this.hasMe = hasMe; }
    public String getMeName() { return meName; }
    public void setMeName(String meName) { this.meName = meName; }
    public int getMeRank() { return meRank; }
    public void setMeRank(int meRank) { this.meRank = meRank; }
    public int getMePoints() { return mePoints; }
    public void setMePoints(int mePoints) { this.mePoints = mePoints; }
    public int getMeTotalPlayers() { return meTotalPlayers; }
    public void setMeTotalPlayers(int meTotalPlayers) { this.meTotalPlayers = meTotalPlayers; }
    public double getMePercentile() { return mePercentile; }
    public void setMePercentile(double mePercentile) { this.mePercentile = mePercentile; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
