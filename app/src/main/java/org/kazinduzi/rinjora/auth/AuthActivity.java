package org.kazinduzi.rinjora.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MainActivity;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.viewmodel.AuthViewModel;
import org.kazinduzi.rinjora.util.AnalyticsHelper;

public class AuthActivity extends BaseActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observe auth state
        authViewModel.getAuthState().observe(this, authState -> {
            if (authState == AuthViewModel.AuthState.AUTHENTICATED) {
                AnalyticsHelper.logEvent(this, "login_success");
                // Navigate to MainActivity
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Load initial fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.auth_container, new AuthSelectionFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to MainActivity if not authenticated
        if (authViewModel.getAuthState().getValue() != AuthViewModel.AuthState.AUTHENTICATED) {
            super.onBackPressed();
        }
    }
}
