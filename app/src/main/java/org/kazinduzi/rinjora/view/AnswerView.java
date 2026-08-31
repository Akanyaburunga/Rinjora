package org.kazinduzi.rinjora.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.util.TextUtil;

/**
 * Reusable lenient-answer input (parity plan §1): a single code path used by both
 * the riddle (Sokwe) and proverb (Heraheza) modes. Encapsulates:
 * <ul>
 *   <li>multi-attempt input — never locks after one wrong guess, only after a correct solve;</li>
 *   <li>concede — typing or tapping "ndaguhaye" submits the raw word (no reward);</li>
 *   <li>reveal — a dimmed, no-reward "learn" action;</li>
 *   <li>uniform result rendering from the shared {@link AnswerResponseDto}.</li>
 * </ul>
 *
 * <p>The view never does its own matching; whatever the player types (including
 * {@code ndaguhaye}) is handed to {@link Listener#onSubmit(String)} verbatim so the
 * host can pass it to the API unchanged. Hosting activities wire {@link #setListener}.
 */
public class AnswerView extends LinearLayout {

    /** Host supplies the network calls; this view stays transport-agnostic. */
    public interface Listener {
        /** Fired with the raw typed text (a plain answer or the literal "ndaguhaye"). */
        void onSubmit(@NonNull String rawAnswer);

        /** Fired on the no-reward reveal ("learn") action. */
        void onReveal();
    }

    private final TextInputEditText etAnswer;
    private final TextView tvConcedeHint;
    private final MaterialCardView resultCard;
    private final TextView tvResultTitle;
    private final TextView tvResultBody;
    private final MaterialButton btnAnswer;
    private final MaterialButton btnReveal;
    private final MaterialButton btnConcede;

    private Listener listener;

    public AnswerView(@NonNull Context context) {
        this(context, null);
    }

    public AnswerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_answer, this, true);

        etAnswer = findViewById(R.id.et_answer);
        tvConcedeHint = findViewById(R.id.tv_concede_hint);
        resultCard = findViewById(R.id.result_card);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultBody = findViewById(R.id.tv_result_body);
        btnAnswer = findViewById(R.id.btn_answer);
        btnReveal = findViewById(R.id.btn_reveal);
        btnConcede = findViewById(R.id.btn_concede);

        btnAnswer.setOnClickListener(v -> submit());
        btnReveal.setOnClickListener(v -> onRevealClicked());
        btnConcede.setOnClickListener(v -> {
            setInputText("ndaguhaye");
            submit();
        });

        // Live concede hint: light up the "Ndaguhaye !" button as soon as the player
        // types something that normalises to the concede word.
        etAnswer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                updateConcedeState();
            }
        });
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public String getInputText() {
        return etAnswer.getText() == null ? "" : etAnswer.getText().toString();
    }

    public void setInputText(@NonNull String text) {
        etAnswer.setText(text);
    }

    public void setConcedeHintVisible(boolean visible) {
        tvConcedeHint.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Show/hide the blocking progress + disable the action buttons while a call is in flight. */
    public void setBusy(boolean busy) {
        btnAnswer.setEnabled(!busy);
        btnReveal.setEnabled(!busy);
        btnConcede.setEnabled(!busy);
        resultCard.setVisibility(busy ? View.GONE : resultCard.getVisibility());
    }

    /** Lock the input + buttons once an item is solved (multi-attempt stays unlocked otherwise). */
    public void setLocked(boolean locked) {
        etAnswer.setEnabled(!locked);
        btnAnswer.setEnabled(!locked);
        btnConcede.setEnabled(!locked);
        if (locked) {
            etAnswer.setText("");
        }
        updateConcedeState();
    }

    /** Render a server grade result (correct / wrong / capped / achievements). */
    public void showResult(@NonNull AnswerResponseDto result) {
        resultCard.setVisibility(View.VISIBLE);
        if (result.isCorrect()) {
            tvResultTitle.setText("Correct!");
            StringBuilder body = new StringBuilder();
            String msg = result.getMessage();
            if (msg != null && !msg.isEmpty()) {
                body.append(msg);
            } else if (result.isRewarded()) {
                body.append("You earned ").append(result.getPoints()).append(" points.");
            }
            if (result.isRewarded() && !result.isCapped()) {
                body.append("\n\n+").append(result.getPoints()).append(" reputation added.");
            } else if (result.isRewarded()) {
                body.append("\n\nPoints capped for today.");
            }
            if (result.getNewAchievements() != null && !result.getNewAchievements().isEmpty()) {
                body.append("\n\nNew achievement unlocked!");
                for (org.kazinduzi.rinjora.network.dto.AchievementDto a : result.getNewAchievements()) {
                    body.append("\n🏅 ").append(a.getName());
                }
            }
            tvResultBody.setText(body.toString());
        } else {
            tvResultTitle.setText("Not quite");
            String body = result.getMessage() != null
                    ? result.getMessage()
                    : "Try again — no points this time.";
            String accepted = TextUtil.normalize(getInputText());
            if (!accepted.isEmpty() && !TextUtil.isConcede(accepted)) {
                body += String.format(Locale.getDefault(), "\n\n(We read: “%s”)", accepted);
            }
            tvResultBody.setText(body);
        }
    }

    /** Render a no-reward, learning-mode reveal. */
    public void showRevealed(@NonNull String answer) {
        resultCard.setVisibility(View.VISIBLE);
        tvResultTitle.setText("Answer (learning mode)");
        tvResultBody.setText("The answer is:\n\n“" + answer + "”\n\nNo points awarded in reveal mode.");
    }

    /** Render an arbitrary host-driven message in the result card. */
    public void showMessage(@NonNull String title, @NonNull String body) {
        resultCard.setVisibility(View.VISIBLE);
        tvResultTitle.setText(title);
        tvResultBody.setText(body);
    }

    /** Reset for the next item: clear input and hide the result card. */
    public void resetForNext() {
        etAnswer.setText("");
        resultCard.setVisibility(View.GONE);
        updateConcedeState();
    }

    private void submit() {
        String raw = getInputText().trim();
        if (raw.isEmpty()) {
            Toast.makeText(getContext(), "Type an answer first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (listener != null) {
            listener.onSubmit(raw);
        }
    }

    private void onRevealClicked() {
        if (listener != null) {
            listener.onReveal();
        }
    }

    private void updateConcedeState() {
        boolean isConcede = TextUtil.isConcede(getInputText());
        btnConcede.setVisibility(isConcede ? View.GONE : View.VISIBLE);
        if (isConcede) {
            btnReveal.setText("Concede");
        } else {
            btnReveal.setText("Reveal (learn)");
        }
    }
}
