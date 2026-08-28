package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A duel participant summary (plan §7.1): id + name + reputation.
 */
public class DuelUserDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("reputation")
    private int reputation;

    public long getId() { return id; }
    public String getName() { return name; }
    public int getReputation() { return reputation; }
}
