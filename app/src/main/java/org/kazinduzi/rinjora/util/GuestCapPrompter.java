package org.kazinduzi.rinjora.util;

import android.content.Context;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;

/**
 * Shared "guest cap reached" prompt (plan §5 §6): when a guest hits the per-mode
 * cap the backend answers {@code 403 requires_registration}. Instead of a bare
 * error toast, offer register / login / later, mirroring the round-game fragments.
 * No pending-mode bookkeeping here: single-item modes just resume when the user
 * re-enters after converting.
 */
public final class GuestCapPrompter {

    private GuestCapPrompter() {
    }

    public static void prompt(Context context, String message) {
        String text = (message == null || message.isEmpty()) ? KirundiUi.G_CAP_MSG : message;
        new MaterialAlertDialogBuilder(context)
                .setTitle(KirundiUi.G_CAP_TITLE)
                .setMessage(text)
                .setPositiveButton(KirundiUi.G_CAP_GO, (d, w) -> open(context, true))
                .setNeutralButton(KirundiUi.G_CAP_LOGIN, (d, w) -> open(context, false))
                .setNegativeButton(KirundiUi.G_CAP_LATER, (d, w) -> {
                })
                .setCancelable(false)
                .show();
    }

    private static void open(Context context, boolean createAccount) {
        Intent intent = new Intent(context, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (createAccount) {
            intent.putExtra(RinjoraAuthActivity.EXTRA_CREATE_ACCOUNT, true);
        }
        context.startActivity(intent);
    }
}