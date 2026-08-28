package org.kazinduzi.rinjora;

import android.content.Intent;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import org.kazinduzi.rinjora.auth.AuthActivity;
import org.kazinduzi.rinjora.databinding.ActivityMainBinding;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isUserAuthenticated()) {
            navigateToAuth();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_repairs, R.id.navigation_notifications,
                R.id.navigation_profile, R.id.navigation_support)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private boolean isUserAuthenticated() {
        try {
            BoxStore boxStore = ((MyApp) getApplication()).getBoxStore();
            Box<User> userBox = boxStore.boxFor(User.class);
            User active = userBox.query().equal(User_.isActive, true).build().findFirst();
            return active != null && active.isTokenValid();
        } catch (Exception e) {
            return false;
        }
    }

    private void navigateToAuth() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
