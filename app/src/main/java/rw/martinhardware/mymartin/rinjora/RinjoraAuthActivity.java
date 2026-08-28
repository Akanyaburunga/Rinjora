package rw.martinhardware.mymartin.rinjora;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import rw.martinhardware.mymartin.BaseActivity;
import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.viewmodel.RinjoraAuthViewModel;

/**
 * Host for the Rinjora (Kazinduzi) register/login flow. Launches
 * {@link RinjoraHomeActivity} once authenticated. Self-contained and separate
 * from the legacy logistics auth so both can coexist during migration.
 */
public class RinjoraAuthActivity extends BaseActivity {

    private RinjoraAuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rinjora_auth);

        viewModel = new ViewModelProvider(this).get(RinjoraAuthViewModel.class);

        viewModel.getAuthState().observe(this, state -> {
            if (state == RinjoraAuthViewModel.RinjoraAuthState.AUTHENTICATED) {
                Intent intent = new Intent(RinjoraAuthActivity.this, RinjoraHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.rinjora_auth_container, new RinjoraLoginFragment())
                    .commit();
        }
    }
}
