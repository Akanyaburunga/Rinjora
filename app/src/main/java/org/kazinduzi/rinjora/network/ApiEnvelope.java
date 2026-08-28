package org.kazinduzi.rinjora.network;

import com.google.gson.annotations.SerializedName;

/**
 * The standard {@code { success, data, message }} envelope returned by every
 * Kazinduzi API endpoint (plan §0). Treat {@code success == false} as an error
 * even when the HTTP status is 200.
 *
 * @param <T> the concrete type of the {@code data} field.
 */
public class ApiEnvelope<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private T data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ApiEnvelope{success=" + success + ", data=" + data + ", message='" + message + "'}";
    }
}
