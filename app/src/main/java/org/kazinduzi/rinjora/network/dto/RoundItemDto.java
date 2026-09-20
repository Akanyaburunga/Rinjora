package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * A single item inside a round (parity plan §5): {@code { type, id, position,
 * question|setup, category, difficulty, options?:[], answered?:bool,
 * answered_correct?:bool, revealed_answer?:String }}.
 *
 * <p>{@code question} carries riddle ({@code sokwe}) and proverb ({@code hera})
 * text; {@code setup} carries the joke ({@code tuja}) opener. {@code options} is
 * present only for tuja. {@code revealed_answer} is non-null only once the item has
 * been answered/conceded server-side (never before).
 */
public class RoundItemDto {

    @SerializedName("type")
    private String type;

    @SerializedName("id")
    private long id;

    @SerializedName("position")
    private int position;

    @SerializedName("question")
    private String question;

    @SerializedName("setup")
    private String setup;

    @SerializedName("category")
    private CategoryDto category;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("options")
    private List<String> options;

    @SerializedName("answered")
    private boolean answered;

    @SerializedName("answered_correct")
    private boolean answeredCorrect;

    @SerializedName("revealed_answer")
    private String revealedAnswer;

    public String getType() { return type; }
    public long getId() { return id; }
    public int getPosition() { return position; }
    public String getQuestion() { return question; }
    public String getSetup() { return setup; }
    public CategoryDto getCategory() { return category; }
    public String getDifficulty() { return difficulty; }

    public List<String> getOptions() {
        return options != null ? options : java.util.Collections.<String>emptyList();
    }

    public boolean isAnswered() { return answered; }
    public boolean isAnsweredCorrect() { return answeredCorrect; }
    public String getRevealedAnswer() { return revealedAnswer; }

    /** The player-facing prompt: {@code question} for quiz modes, {@code setup} for tuja. */
    public String getText() {
        if (question != null && !question.trim().isEmpty()) return question;
        return setup != null ? setup : "";
    }
}