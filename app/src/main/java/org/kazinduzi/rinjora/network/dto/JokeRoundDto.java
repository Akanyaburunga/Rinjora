package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Payload of {@code GET /jokes/round} (parity plan §3.2). The {@code options} arrive
 * already server-shuffled and include the correct punchline exactly once — the client
 * must <b>not</b> re-sort them; render in the given order.
 */
public class JokeRoundDto {

    @SerializedName("joke_id")
    private long jokeId;

    @SerializedName("setup")
    private String setup;

    @SerializedName("options")
    private List<String> options;

    public long getJokeId() {
        return jokeId;
    }

    public String getSetup() {
        return setup;
    }

    public List<String> getOptions() {
        return options != null ? options : Collections.<String>emptyList();
    }
}
