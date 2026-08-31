package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A single Tujajure attempt in {@code GET /jokes/history} (parity plan §4.1):
 * {@code { id, joke, chosen_option, is_correct, rewarded, attempted_at }}. The
 * {@code joke} is a {@link JokeRoundDto}; only its safe fields (setup/options) are
 * rendered, never a standalone punchline.
 */
public class JokeHistoryEntryDto {

    @SerializedName("id")
    private long id;

    @SerializedName("joke")
    private JokeRoundDto joke;

    @SerializedName("chosen_option")
    private String chosenOption;

    @SerializedName("is_correct")
    private boolean correct;

    @SerializedName("rewarded")
    private boolean rewarded;

    @SerializedName("attempted_at")
    private String attemptedAt;

    public long getId() { return id; }
    public JokeRoundDto getJoke() { return joke; }
    public String getChosenOption() { return chosenOption; }
    public boolean isCorrect() { return correct; }
    public boolean isRewarded() { return rewarded; }
    public String getAttemptedAt() { return attemptedAt; }
}
