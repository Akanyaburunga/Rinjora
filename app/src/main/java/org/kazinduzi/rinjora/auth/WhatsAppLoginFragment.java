package org.kazinduzi.rinjora.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentWhatsappLoginBinding;
import org.kazinduzi.rinjora.viewmodel.AuthViewModel;

public class WhatsAppLoginFragment extends Fragment {

    private FragmentWhatsappLoginBinding binding;
    private AuthViewModel authViewModel;
    private String phoneNumber;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWhatsappLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        binding.btnRequestOtp.setOnClickListener(v -> {
            String phone = binding.etPhoneNumber.getText().toString().trim();
            if (validatePhone(phone)) {
                phoneNumber = phone;
                authViewModel.requestWhatsAppOtp(phoneNumber);
            }
        });

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            if (validateOtp(otp)) {
                authViewModel.verifyWhatsAppOtp(phoneNumber, otp);
            }
        });

        binding.btnResend.setOnClickListener(v -> {
            if (phoneNumber != null) {
                authViewModel.requestWhatsAppOtp(phoneNumber);
                Toast.makeText(getContext(), "OTP resent", Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnRequestOtp.setEnabled(!loading);
            binding.btnVerify.setEnabled(!loading);
            binding.btnResend.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        });

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            if (state == AuthViewModel.AuthState.OTP_SENT) {
                showOtpInput();
            }
        });
    }

    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            binding.etPhoneNumber.setError("Phone number is required");
            return false;
        }
        if (!phone.matches("^[+]?[0-9]{10,15}$")) {
            binding.etPhoneNumber.setError("Invalid phone number");
            return false;
        }
        return true;
    }

    private boolean validateOtp(String otp) {
        if (TextUtils.isEmpty(otp)) {
            binding.etOtp.setError("OTP is required");
            return false;
        }
        if (otp.length() < 4 || otp.length() > 6) {
            binding.etOtp.setError("OTP must be 4-6 digits");
            return false;
        }
        return true;
    }

    private void showOtpInput() {
        binding.tvOtpSection.setVisibility(View.VISIBLE);
        binding.tvPhoneDisplay.setText(phoneNumber);
        binding.tvPhoneDisplay.setVisibility(View.VISIBLE);
        binding.layoutOtp.setVisibility(View.VISIBLE);
        binding.btnVerify.setVisibility(View.VISIBLE);
        binding.btnResend.setVisibility(View.VISIBLE);
        binding.btnRequestOtp.setVisibility(View.GONE);
        binding.layoutPhone.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
