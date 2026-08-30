package org.kazinduzi.rinjora;

import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.kazinduzi.rinjora.databinding.ActivityMainBinding;

/**
 * Launcher shell for the Rinjora game. Guests can play without an account; progress
 * is stored locally (ObjectBox) and synced once they create one (see
 * {@link org.kazinduzi.rinjora.data.GuestProgressRepository}). Tabbed bottom nav:
 * Sokwe (play), Heraheza (fill-the-blank), Tujajure (jokes/duels), Jewe (me/profile).
 */
public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_sokwe, R.id.navigation_heraheza, R.id.navigation_tujajure,
                R.id.navigation_jewe)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}
