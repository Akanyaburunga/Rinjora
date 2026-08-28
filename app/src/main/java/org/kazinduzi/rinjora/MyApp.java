package org.kazinduzi.rinjora;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import io.objectbox.BoxStore;
import org.kazinduzi.rinjora.data.HomeSyncWorker;
import org.kazinduzi.rinjora.data.ProfileSyncWorker;
import org.kazinduzi.rinjora.entities.MyObjectBox;

public class MyApp extends Application {

    private BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();
        boxStore = MyObjectBox.builder().androidContext(this).build();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Disable dark mode
        scheduleHomeSync();
        scheduleProfileSync();
    }

    private void scheduleHomeSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(HomeSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                HomeSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }

    private void scheduleProfileSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ProfileSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                ProfileSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }
}
