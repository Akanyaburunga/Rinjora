package org.kazinduzi.rinjora.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentAuthSelectionBinding;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;

public class AuthSelectionFragment extends Fragment {

    private FragmentAuthSelectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Email login button
        binding.btnEmailLogin.setOnClickListener(v -> {
            navigateToFragment(new EmailLoginFragment());
        });

        // WhatsApp login button
        binding.btnWhatsappLogin.setOnClickListener(v -> {
            navigateToFragment(new WhatsAppLoginFragment());
        });

        // Phone login button
        binding.btnPhoneLogin.setOnClickListener(v -> {
            navigateToFragment(new PhoneLoginFragment());
        });

        // Rinjora (Kazinduzi) games — separate, self-contained flow
        binding.btnRinjoraLogin.setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireContext(), RinjoraAuthActivity.class));
        });
    }

    private void navigateToFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.auth_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
