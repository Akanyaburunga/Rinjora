package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response body of {@code POST /riddles/{id}/reveal} (plan §2.9 educational/learning
 * mode). Returns the full question and answer with <b>no reward</b>.
 */
public class RevealDto {

    @SerializedName("id")
    private long id;

    @SerializedName("question")
    private String question;

    @SerializedName("answer")
    private String answer;

    public long getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
}
