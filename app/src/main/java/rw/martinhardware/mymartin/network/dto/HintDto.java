package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response body of {@code GET /riddles/{id}/hint} (plan §2.8). Reveals the next
 * hint progressively and reports how many hints have now been revealed.
 */
public class HintDto {

    @SerializedName("id")
    private long id;

    @SerializedName("hint")
    private String hint;

    @SerializedName("hint2")
    private String hint2;

    @SerializedName("hints_revealed")
    private int hintsRevealed;

    public long getId() { return id; }
    public String getHint() { return hint; }
    public String getHint2() { return hint2; }
    public int getHintsRevealed() { return hintsRevealed; }
}
