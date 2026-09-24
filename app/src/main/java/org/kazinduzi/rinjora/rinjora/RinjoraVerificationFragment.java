package org.kazinduzi.rinjora.rinjora;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentRinjoraVerificationBinding;
import org.kazinduzi.rinjora.viewmodel.RinjoraAuthViewModel;

import java.util.Locale;

/**
 * Email-verification screen (docs mobile-api-email-verification.md §3.2): the user
 * types the emailed 6-digit code. On success the user is auto-logged-in (fresh
 * registration) or sent back to the login form with the email prefilled (the
 * post-login 403 gate). Also offers resend and a 10-minute expiry countdown.
 */
public class RinjoraVerificationFragment extends Fragment {

    private static final long CODE_TTL_MS = 10 * 60 * 1000L;

    private FragmentRinjoraVerificationBinding binding;
    private RinjoraAuthViewModel viewModel;
    private CountDownTimer countDownTimer;
    private boolean successHandled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRinjoraVerificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(RinjoraAuthViewModel.class);

        String email = viewModel.getPendingEmail().getValue();
        binding.tvEmailDisplay.setText(email == null ? "" : email);

        binding.etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.tvCodeError.getVisibility() == View.VISIBLE) {
                    binding.tvCodeError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnVerify.setOnClickListener(v -> {
            String code = binding.etCode.getText() == null ? "" : binding.etCode.getText().toString().trim();
            if (validateCode(code)) {
                viewModel.verifyEmail(code);
            }
        });

        binding.linkResend.setOnClickListener(v -> viewModel.resendVerificationCode());

        binding.linkLogin.setOnClickListener(v -> goToLogin());

        binding.btnBack.setOnClickListener(v -> {
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                goToLogin();
            }
        });

        startCountdown();

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.btnVerify.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getVerificationError().observe(getViewLifecycleOwner(), message -> {
            if (binding == null) return;
            if (message != null) {
                binding.tvCodeError.setText(message);
                binding.tvCodeError.setVisibility(View.VISIBLE);
            } else {
                binding.tvCodeError.setVisibility(View.GONE);
            }
        });

        viewModel.getResendMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                startCountdown();
            }
        });

        viewModel.getVerificationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (!Boolean.TRUE.equals(success) || successHandled || binding == null) {
                return;
            }
            successHandled = true;
            stopCountdown();
            viewModel.clearVerificationSuccess();
            if (viewModel.isAutoLoginAfterVerification()) {
                // Auto-login is already in flight; the auth host will land on Home.
                Toast.makeText(getContext(), R.string.ev_verify_done, Toast.LENGTH_SHORT).show();
            } else {
                // No remembered password (post-login 403 gate): go to login, email prefilled.
                goToLogin();
            }
        });
    }

    private boolean validateCode(String code) {
        if (code.length() != 6) {
            binding.tvCodeError.setText(R.string.ev_code_short);
            binding.tvCodeError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!code.matches("[0-9]{6}")) {
            binding.tvCodeError.setText(R.string.ev_code_short);
            binding.tvCodeError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.tvCodeError.setVisibility(View.GONE);
        return true;
    }

    private void goToLogin() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.rinjora_auth_container, new RinjoraLoginFragment())
                .commit();
    }

    private void startCountdown() {
        stopCountdown();
        countDownTimer = new CountDownTimer(CODE_TTL_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding != null) {
                    binding.tvCountdown.setText(
                            getString(R.string.ev_countdown) + " " + format(millisUntilFinished));
                }
            }

            @Override
            public void onFinish() {
                if (binding != null) {
                    binding.tvCountdown.setText(getString(R.string.ev_countdown) + " 00:00");
                    binding.tvCodeError.setText(R.string.ev_expired);
                    binding.tvCodeError.setVisibility(View.VISIBLE);
                }
            }
        }.start();
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private static String format(long millis) {
        long totalSeconds = millis / 1000L;
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCountdown();
        binding = null;
    }
}