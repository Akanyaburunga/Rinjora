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
import org.kazinduzi.rinjora.databinding.FragmentPhoneLoginBinding;
import org.kazinduzi.rinjora.viewmodel.AuthViewModel;

public class PhoneLoginFragment extends Fragment {

    private FragmentPhoneLoginBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPhoneLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        binding.btnVerifyPhone.setOnClickListener(v -> {
            String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

            if (validateInput(phoneNumber)) {
                // In a real implementation, you would integrate Firebase Phone Auth here
                // For now, we'll simulate it with a mock token
                String mockFirebaseToken = "mock_firebase_token_" + phoneNumber;
                authViewModel.verifyFirebasePhone(mockFirebaseToken);
            }
        });

        // Observe loading state
        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnVerifyPhone.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe errors
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            binding.etPhoneNumber.setError("Phone number is required");
            return false;
        }

        if (!phoneNumber.matches("^[+]?[0-9]{10,15}$")) {
            binding.etPhoneNumber.setError("Invalid phone number format");
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
