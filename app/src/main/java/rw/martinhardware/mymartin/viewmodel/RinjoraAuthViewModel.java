package rw.martinhardware.mymartin.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import rw.martinhardware.mymartin.data.RinjoraAuthRepository;
import rw.martinhardware.mymartin.network.AuthTokenStore;

/**
 * Auth state machine for the Rinjora (Kazinduzi) login flow (plan §1).
 *
 * Mirrors the existing {@link AuthViewModel} shape (LiveData state/loading/error)
 * but drives the new Retrofit {@link RinjoraAuthRepository} and stores the token
 * in {@link AuthTokenStore}.
 */
public class RinjoraAuthViewModel extends AndroidViewModel {

    public enum RinjoraAuthState {
        NOT_AUTHENTICATED,
        AUTHENTICATED
    }

    private final MutableLiveData<RinjoraAuthState> authState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private final RinjoraAuthRepository repository;

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

    public void checkAuthStatus() {
        authState.setValue(
                AuthTokenStore.get(getApplication()).hasValidToken()
                        ? RinjoraAuthState.AUTHENTICATED
                        : RinjoraAuthState.NOT_AUTHENTICATED);
    }

    public void login(String email, String password) {
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
                // Plan §1.1: registration returns no token; the user must log in
                // (or verify email in production). Stay on the login screen.
                errorMessage.setValue(null);
                authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void logout() {
        repository.logout(new RinjoraAuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            }

            @Override
            public void onError(String message) {
                authState.setValue(RinjoraAuthState.NOT_AUTHENTICATED);
            }
        });
    }
}
