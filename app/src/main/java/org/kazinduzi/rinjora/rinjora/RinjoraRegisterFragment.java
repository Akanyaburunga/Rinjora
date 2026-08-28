package org.kazinduzi.rinjora.rinjora;

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
import org.kazinduzi.rinjora.databinding.FragmentRinjoraRegisterBinding;
import org.kazinduzi.rinjora.viewmodel.RinjoraAuthViewModel;

public class RinjoraRegisterFragment extends Fragment {

    private FragmentRinjoraRegisterBinding binding;
    private RinjoraAuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRinjoraRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(RinjoraAuthViewModel.class);

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirm = binding.etConfirm.getText().toString().trim();
            if (validate(name, email, password, confirm)) {
                viewModel.register(name, email, password, confirm);
            }
        });

        binding.linkLogin.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnRegister.setEnabled(!loading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validate(String name, String email, String password, String confirm) {
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email format");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            return false;
        }
        if (!password.equals(confirm)) {
            binding.etConfirm.setError("Passwords do not match");
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
