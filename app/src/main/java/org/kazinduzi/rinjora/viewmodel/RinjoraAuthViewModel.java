package org.kazinduzi.rinjora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.kazinduzi.rinjora.data.RinjoraAuthRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.util.KirundiUi;

/**
 * Auth state machine for the Rinjora (Kazinduzi) login flow (plan §1) including
 * the 6-digit email-verification flow (docs mobile-api-email-verification.md).
 *
 * Mirrors the existing {@link AuthViewModel} shape (LiveData state/loading/error)
 * but drives the new Retrofit {@link RinjoraAuthRepository} and stores the token
 * in {@link AuthTokenStore}.
 */
public class RinjoraAuthViewModel extends AndroidViewModel {

    public enum RinjoraAuthState {
        NOT_AUTHENTICATED,
        /** A fresh registration succeeded; the Enter Code screen must be shown. */
        VERIFICATION_REQUIRED,
        AUTHENTICATED
    }

    private final MutableLiveData<RinjoraAuthState> authState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> pendingEmail = new MutableLiveData<>();
    private final MutableLiveData<String> verificationError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> verificationSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> resendMessage = new MutableLiveData<>();

    private final RinjoraAuthRepository repository;

    /** Password captured at registration for an automatic login once verified. */
    private String pendingPassword;
    /** True while an automatic post-verification login is in flight. */
    private boolean autoLoginAfterVerification;

    public RinjoraAuthViewModel(@NonNull Application application) {
        super(application);
        this.repository = new RinjoraAuthRepository(application);
        checkAuthStatus();
    }

    public LiveData<RinjoraAuthState> getAuthState() {
        return authState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getPendingEmail() {
        return pendingEmail;
    }

    public LiveData<String> getVerificationError() {
        return verificationError;
    }

    public LiveData<Boolean> getVerificationSuccess() {
        return verificationSuccess;
    }

    public LiveData<String> getResendMessage() {
        return resendMessage;
    }

    public void setPendingEmail(String email) {
        pendingEmail.setValue(email);
    }

    public void clearVerificationSuccess() {
        verificationSuccess.setValue(false);
    }

    public boolean isAutoLoginAfterVerification() {
        return autoLoginAfterVerification;
    }

    public void checkAuthStatus() {
        AuthTokenStore store = AuthTokenStore.get(getApplication());
        if (store.isEmailVerificationRequired()) {
            // A 403-unverified gate cleared the token; land on Enter Code, not home.
            authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            return;
        }
        authState.setValue(
                store.hasValidToken()
                        ? RinjoraAuthState.AUTHENTICATED
                        : RinjoraAuthState.NOT_AUTHENTICATED);
    }

    public void login(String email, String password) {
        // A fresh, deliberate login supersedes any pending verification gate.
        AuthTokenStore.get(getApplication()).setEmailVerificationRequired(false);
        autoLoginAfterVerification = false;
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.login(email, password, new RinjoraAuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                authState.setValue(RinjoraAuthState.AUTHENTICATED);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void register(String name, String email, String password, String confirm) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.register(name, email, password, confirm, new RinjoraAuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                // Registration returns no token: a 6-digit code has been dispatched
                // (email in staging/prod, backend log in local dev — docs §4) and the
                // Enter Code screen must confirm it before login (docs §2.1). The
                // pending email/state is persisted so resend works across restarts.
                AuthTokenStore store = AuthTokenStore.get(getApplication());
                store.saveEmail(email);
                store.setEmailVerificationRequired(true);
                pendingPassword = password;
                autoLoginAfterVerification = false;
                pendingEmail.setValue(email);
                errorMessage.setValue(null);
                authState.setValue(RinjoraAuthState.VERIFICATION_REQUIRED);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    /**
     * Submits the emailed 6-digit code for {@link #pendingEmail}. On success the
     * user is either logged in automatically (regular registration knows the
     * password) or sent back to the login form with the email prefilled (the
     * post-login 403 gate does not).
     */
    public void verifyEmail(String verificationCode) {
        String email = pendingEmail.getValue();
        if (email == null || email.isEmpty()) {
            verificationError.setValue(KirundiUi.EV_CODE_MISSING);
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);
        verificationError.setValue(null);
        repository.verifyEmail(email, verificationCode, new RinjoraAuthRepository.VerifyEmailCallback() {
            @Override
            public void onSuccess() {
                afterVerificationSuccess();
            }

            @Override
            public void onInvalidCode() {
                isLoading.setValue(false);
                verificationError.setValue(KirundiUi.EV_INVALID);
            }

            @Override
            public void onExpiredCode() {
                isLoading.setValue(false);
                verificationError.setValue(KirundiUi.EV_EXPIRED);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    /** Re-emails the code for {@link #pendingEmail} and restarts the 10-minute window. */
    public void resendVerificationCode() {
        String email = pendingEmail.getValue();
        if (email == null || email.isEmpty()) {
            errorMessage.setValue(KirundiUi.EV_CODE_MISSING);
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.resendVerificationCode(email, new RinjoraAuthRepository.ResendCallback() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                AuthTokenStore.get(getApplication()).setEmailVerificationRequired(false);
                if (message.toLowerCase().contains("already verified")) {
                    // Nothing to do — treat as a pass state (docs §2.3).
                    afterVerificationSuccess();
                } else {
                    resendMessage.setValue(KirundiUi.EV_RESENT);
                }
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    private void afterVerificationSuccess() {
        AuthTokenStore.get(getApplication()).setEmailVerificationRequired(false);
        String email = pendingEmail.getValue();
        String password = pendingPassword;
        pendingPassword = null;
        boolean autoLogin = email != null && password != null;
        // Must be set before verificationSuccess is delivered: the fragment decides
        // how to continue from this flag, and login() resets it below.
        autoLoginAfterVerification = autoLogin;
        isLoading.setValue(false);
        verificationError.setValue(null);
        verificationSuccess.setValue(true);
        if (autoLogin) {
            login(email, password);
        }
    }

    public void logout() {
        repository.logout(new RinjoraAuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                pendingEmail.setValue(null);
                pendingPassword = null;
                autoLoginAfterVerification = false;
                authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            }

            @Override
            public void onError(String message) {
                pendingEmail.setValue(null);
                pendingPassword = null;
                autoLoginAfterVerification = false;
                authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            }
        });
    }
}
