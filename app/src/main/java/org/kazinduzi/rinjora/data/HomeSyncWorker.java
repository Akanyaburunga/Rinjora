package org.kazinduzi.rinjora.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kazinduzi.rinjora.entities.HomeSnapshot;

/**
 * Background sync for the driver home dashboard. Scheduled periodically in MyApp.
 * Offline-first: a failed sync keeps the cached snapshot and is simply retried on
 * the next period — the UI never depends on this worker succeeding.
 */
public class HomeSyncWorker extends Worker {

    public static final String WORK_NAME = "home_sync";
    private static final long FETCH_TIMEOUT_SECONDS = 45;

    public HomeSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        final CountDownLatch latch = new CountDownLatch(1);

        new DriverHomeRepository().fetch(getApplicationContext(), new DriverHomeRepository.Callback() {
            @Override
            public void onSuccess(HomeSnapshot snapshot, boolean idle) {
                latch.countDown();
            }

            @Override
            public void onAuthError() {
                // Cache stays; next periodic run retries once the session is refreshed.
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                latch.countDown();
            }
        });

        try {
            latch.await(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.success();
    }
}
