package org.kazinduzi.rinjora.network;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * HTTP response logger that redacts the confidential {@code answer} and
 * {@code submitted_answer} JSON values before printing, so a revealed riddle
 * answer is <b>never</b> written to logcat. The redaction only affects the log
 * output — the body handed to the app is the original, un-redacted bytes.
 *
 * <p>Request *bodies* are intentionally not logged at all, so the answer a player
 * submits can never leak either. The response body is logged unconditionally (both
 * debug and release) so a parse failure such as "Use JsonReader.setLenient(true)"
 * can be diagnosed against the exact JSON the server actually sent, while the
 * {@code answer} / {@code submitted_answer} values stay redacted.
 */
public final class RedactingLoggingInterceptor implements Interceptor {

    private static final Pattern ANSWER_VALUE = Pattern.compile(
            "(\\\"(?:answer|submitted_answer)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final String TAG = "RinjoraHTTP";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        android.util.Log.d(TAG, "--> " + request.method() + " " + request.url().toString());

        Response response = chain.proceed(request);

        ResponseBody original = response.body();
        if (original == null) {
            android.util.Log.w(TAG, "<-- " + response.code() + " " + response.request().url()
                    + " (EMPTY BODY / no body)");
            return response;
        }

        // Read the whole body, preserving the original bytes for the app.
        Buffer buffer = new Buffer();
        original.source().readAll(buffer);
        String bodyString = buffer.readUtf8();

        android.util.Log.d(TAG, "<-- " + response.code() + " " + response.request().url().toString());
        if (bodyString == null || bodyString.trim().isEmpty()) {
            android.util.Log.w(TAG, "response body: <EMPTY/WHITESPACE-ONLY> (this is NOT valid JSON)");
        } else {
            android.util.Log.d(TAG, "response body: " + redact(bodyString));
        }

        // Re-wrap with the ORIGINAL bytes so the app receives the true answer.
        ResponseBody wrapped = ResponseBody.create(original.contentType(), bodyString);
        return response.newBuilder().body(wrapped).build();
    }

    private static String redact(String json) {
        if (json == null) return null;
        // Replace "answer":"<value>" and "submitted_answer":"<value>" values.
        // Answers are always JSON strings, so a plain-string-safe replace works.
        return ANSWER_VALUE.matcher(json).replaceAll("$1***$2");
    }
}
