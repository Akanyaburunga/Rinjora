package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Body of {@code POST /riddles/{id}/share} (plan §6.2): a short share URL + code
 * that can be handed to the Android share-sheet.
 */
public class ShareDto {

    @SerializedName("share_url")
    private String shareUrl;

    @SerializedName("code")
    private String code;

    public String getShareUrl() { return shareUrl; }
    public String getCode() { return code; }
}
