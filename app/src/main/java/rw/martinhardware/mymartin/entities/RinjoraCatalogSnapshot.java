package rw.martinhardware.mymartin.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for the riddle catalog (plan Phase 10/K): the {@code GET
 * /riddles} list per difficulty/type filter and the {@code GET /riddles/categories}
 * list. Each row stores the raw JSON envelope data so the Play screen renders
 * instantly from cache when offline, then refreshes in the background.
 *
 * <p>Only public (non-confidential) fields are stored — the catalog is what a
 * player would already see; the answer is never persisted here.
 */
@Entity
public class RinjoraCatalogSnapshot {

    public static final String KIND_RIDDLES = "riddles";
    public static final String KIND_CATEGORIES = "categories";

    @Id
    public long id;

    /** {@link #KIND_RIDDLES} or {@link #KIND_CATEGORIES}. */
    public String kind;

    /** The difficulty/type filter key (e.g. "all", "easy@riddle"); "categories" for the category list. */
    public String key;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    /** Raw {@code List<RiddleDto>} or {@code List<CategoryDto>} JSON (data of the envelope). */
    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
