package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MainActivity;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.viewmodel.RinjoraAuthViewModel;

/**
 * Host for the Rinjora (Kazinduzi) register/login/email-verification flow.
 * Launches {@link MainActivity} (Sokwe tab) once authenticated. Self-contained
 * and separate from the legacy logistics auth so both can coexist during migration.
 */
public class RinjoraAuthActivity extends BaseActivity {

    /**
     * Optional extra: this launch came from the guest cap prompt (plan §5), so the
     * user went straight to account creation. The guest bearer token is still valid,
     * so the {@code AUTHENTICATED} auto-bounce must be suppressed until a real login
     * replaces it — otherwise the host would instantly jump back to MainActivity.
     */
    public static final String EXTRA_CREATE_ACCOUNT = "org.kazinduzi.rinjora.extra.CREATE_ACCOUNT";

    private boolean forceCreateAccount;
    private RinjoraAuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forceCreateAccount = getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_CREATE_ACCOUNT, false);
        setContentView(R.layout.activity_rinjora_auth);

        viewModel = new ViewModelProvider(this).get(RinjoraAuthViewModel.class);

        viewModel.getAuthState().observe(this, state -> {
            if (state == null) return;
            if (state == RinjoraAuthViewModel.RinjoraAuthState.AUTHENTICATED) {
                AuthTokenStore store = AuthTokenStore.get(this);
                // The auth host can be launched while a valid guest token is present
                // (cap prompt). Only a real login flips isGuest to false; until then a
                // valid token is expected and must NOT bounce back to MainActivity.
                if (store.hasValidToken() && store.isGuest()) {
                    return;
                }
                Intent intent = new Intent(RinjoraAuthActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (state == RinjoraAuthViewModel.RinjoraAuthState.VERIFICATION_REQUIRED) {
                // Fresh registration: drop the register back stack, show Enter Code.
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.rinjora_auth_container, new RinjoraVerificationFragment())
                        .commit();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.rinjora_auth_container, initialFragment())
                    .commit();
        }
    }

    /**
     * A 403 {@code "Your email address is not verified."} response routes here with
     * the account flagged (docs §3.3): land straight on the Enter Code screen so the
     * user finishes verification before re-logging in. The guest cap prompt instead
     * lands on account creation (plan §5).
     */
    private Fragment initialFragment() {
        AuthTokenStore store = AuthTokenStore.get(this);
        if (store.isEmailVerificationRequired() && !TextUtils.isEmpty(store.getEmail())) {
            viewModel.setPendingEmail(store.getEmail());
            return new RinjoraVerificationFragment();
        }
        if (forceCreateAccount && store.hasValidToken() && store.isGuest()) {
            return new RinjoraRegisterFragment();
        }
        return new RinjoraLoginFragment();
    }
}
