package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Offline-first cache for the current Tujajure joke round (parity plan §3). Lets the
 * round screen survive rotation and short offline gaps.
 * <p>
 * Only the {@code setup} and the 4 server-shuffled {@code options} are persisted, in
 * exactly the server's order. The correct punchline is never stored as a distinct
 * field — it lives only inside the (indistinguishable) {@code options} list, so the
 * cached content cannot leak "the answer" any more than the on-screen round itself does.
 */
@Entity
public class RinjoraJokeSnapshot {

    @Id
    public long id;

    /** The joke's backend id ({@code joke_id}). */
    public long jokeId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    public String setup;

    /** 4 options serialized as a JSON string, server order preserved. */
    public String optionsJson;

    public boolean solved;

    public String rawJson;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getJokeId() { return jokeId; }
    public void setJokeId(long jokeId) { this.jokeId = jokeId; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getSetup() { return setup; }
    public void setSetup(String setup) { this.setup = setup; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
