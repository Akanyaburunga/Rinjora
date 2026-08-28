package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.kazinduzi.rinjora.databinding.ActivityRinjoraContributeBinding;
import org.kazinduzi.rinjora.data.RinjoraSubmissionRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.SubmissionDto;

/**
 * Rinjora "Contribute a riddle" form (plan Phase J, §8.1): submits a new
 * Kinyarwanda riddle to {@code POST /submissions/riddles} for admin review.
 * A {@code 422} (e.g. the answer already exists) is surfaced as an error toast.
 */
public class RinjoraContributeActivity extends AppCompatActivity {

    private ActivityRinjoraContributeBinding binding;
    private RinjoraSubmissionRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraContributeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            finish();
            return;
        }

        repository = new RinjoraSubmissionRepository(this);
        binding.btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String question = binding.etQuestion.getText().toString().trim();
        String answer = binding.etAnswer.getText().toString().trim();
        String difficulty = binding.etDifficulty.getText().toString().trim();
        String riddleType = binding.etRiddleType.getText().toString().trim();
        String source = binding.etSource.getText().toString().trim();

        if (question.isEmpty() || answer.isEmpty()) {
            Toast.makeText(this, "Enter a riddle question and its answer.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (source.isEmpty()) {
            Toast.makeText(this, "A source is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (difficulty.isEmpty()) {
            difficulty = "medium";
        }
        if (riddleType.isEmpty()) {
            riddleType = "classic";
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        repository.create(question, answer, difficulty, riddleType,
                binding.etHint.getText().toString(),
                binding.etHint2.getText().toString(),
                source,
                new RinjoraSubmissionRepository.Callback<SubmissionDto>() {
                    @Override
                    public void onSuccess(SubmissionDto submission) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(RinjoraContributeActivity.this,
                                "Submitted for review. Thanks for contributing!",
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RinjoraContributeActivity.this,
                                RinjoraSubmissionsActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthError() {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(RinjoraContributeActivity.this, "Session expired.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(RinjoraContributeActivity.this, message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
