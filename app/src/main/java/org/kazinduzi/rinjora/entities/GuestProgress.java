package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

/**
 * Fine-grained progress record created while playing as a guest. Each row is the
 * smallest unit that gets replayed against the server once the guest creates an
 * account ("play now, sync later").
 *
 * <p>A {@code dirty} flag marks rows that have not been uploaded yet; the sync flow
 * reads every dirty row, posts it to the matching endpoint, then clears the flag
 * (or removes the row). Nothing here is confidential — a submitted answer is never
 * persisted (see {@code RinjoraRiddleSnapshot}).
 */
@Entity
public class GuestProgress {

    /** A solved riddle attempt. */
    public static final String KIND_RIDDLE_ATTEMPT = "riddle_attempt";
    /** A completed daily riddle. */
    public static final String KIND_DAILY_ATTEMPT = "daily_attempt";
    /** A finished Heraheza (fill-the-blank) level. */
    public static final String KIND_HERAHEZA = "heraheza";
    /** A duel play. */
    public static final String KIND_DUEL = "duel";

    @Id
    public long id;

    /** One of {@link #KIND_RIDDLE_ATTEMPT} etc. */
    @Index
    public String kind;

    /** Server/external identifier this record refers to (e.g. a riddle id), if any. */
    @Index
    public String refId;

    /** Points earned by this record. */
    public long points;

    /** Whether the play was successful/correct. */
    public boolean correct;

    /** ms epoch when the record was created (offline). */
    public long happenedAt;

    /** True until this record has been uploaded to the server (sync pending). */
    public boolean dirty;

    // Getters & setters -----------------------------------------------------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public long getHappenedAt() { return happenedAt; }
    public void setHappenedAt(long happenedAt) { this.happenedAt = happenedAt; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
}
