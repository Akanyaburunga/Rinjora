package org.kazinduzi.rinjora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final Box<User> userBox;
    private final VolleySingleton volley;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        BoxStore boxStore = ((MyApp) application).getBoxStore();
        this.userBox = boxStore.boxFor(User.class);
        this.volley = VolleySingleton.getInstance(application);
        checkAuthStatus();
    }

    public LiveData<AuthState> getAuthState() { return authState; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void checkAuthStatus() {
        User active = getActiveUser();
        if (active != null && active.isTokenValid()) {
            authState.setValue(AuthState.AUTHENTICATED);
        } else {
            authState.setValue(AuthState.NOT_AUTHENTICATED);
        }
    }

    public void requestWhatsAppOtp(String phoneNumber) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        JSONObject body = new JSONObject();
        try { body.put("identifier", phoneNumber); } catch (JSONException e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.AUTH_REQUEST_WHATSAPP_OTP, body,
                response -> {
                    isLoading.setValue(false);
                    authState.setValue(AuthState.OTP_SENT);
                },
                error -> {
                    isLoading.setValue(false);
                    this.errorMessage.setValue(parseError(error));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    public void verifyWhatsAppOtp(String phoneNumber, String otp) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        JSONObject body = new JSONObject();
        try {
            body.put("identifier", phoneNumber);
            body.put("code", otp);
        } catch (JSONException e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.AUTH_VERIFY_WHATSAPP_OTP, body,
                response -> {
                    isLoading.setValue(false);
                    saveUserFromResponse(response, phoneNumber);
                    authState.setValue(AuthState.AUTHENTICATED);
                },
                error -> {
                    isLoading.setValue(false);
                    this.errorMessage.setValue(parseError(error));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    public void verifyFirebasePhone(String firebaseToken) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        JSONObject body = new JSONObject();
        try { body.put("firebase_token", firebaseToken); } catch (JSONException e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.AUTH_VERIFY_FIREBASE_PHONE, body,
                response -> {
                    isLoading.setValue(false);
                    saveUserFromResponse(response, null);
                    authState.setValue(AuthState.AUTHENTICATED);
                },
                error -> {
                    isLoading.setValue(false);
                    this.errorMessage.setValue(parseError(error));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    public void loginWithEmail(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.BASE_URL + "/auth/login", body,
                response -> {
                    isLoading.setValue(false);
                    saveUserFromResponse(response, null);
                    User u = getActiveUser();
                    if (u != null) u.setEmail(email);
                    authState.setValue(AuthState.AUTHENTICATED);
                },
                error -> {
                    isLoading.setValue(false);
                    this.errorMessage.setValue(parseError(error));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    public void logout() {
        isLoading.setValue(true);
        User active = getActiveUser();
        if (active != null && active.getToken() != null) {
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, ApiConfig.AUTH_LOGOUT, null,
                    response -> {
                        isLoading.setValue(false);
                        clearActiveUser();
                        authState.setValue(AuthState.NOT_AUTHENTICATED);
                    },
                    error -> {
                        isLoading.setValue(false);
                        clearActiveUser();
                        authState.setValue(AuthState.NOT_AUTHENTICATED);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", "Bearer " + active.getToken());
                    h.put("Accept", "application/json");
                    return h;
                }
            };
            volley.addToRequestQueue(request);
        } else {
            clearActiveUser();
            isLoading.setValue(false);
            authState.setValue(AuthState.NOT_AUTHENTICATED);
        }
    }

    public void resetAuthState() {
        authState.setValue(AuthState.NOT_AUTHENTICATED);
        errorMessage.setValue(null);
    }

    // --- private helpers ---

    private User getActiveUser() {
        return userBox.query().equal(User_.isActive, true).build().findFirst();
    }

    private void clearActiveUser() {
        User u = getActiveUser();
        if (u != null) {
            u.setActive(false);
            u.setToken(null);
            u.setRefreshToken(null);
            userBox.put(u);
        }
    }

    private void saveUserFromResponse(JSONObject response, String phoneNumber) {
        try {
            User user = new User();
            if (response.has("user")) {
                JSONObject u = response.getJSONObject("user");
                if (u.has("uuid")) user.setUuid(u.getString("uuid"));
                if (u.has("name")) user.setName(u.getString("name"));
                if (u.has("email")) user.setEmail(u.getString("email"));
                if (u.has("phone_number")) user.setPhoneNumber(u.getString("phone_number"));
            }
            if (response.has("token")) user.setToken(response.getString("token"));
            if (response.has("refresh_token")) user.setRefreshToken(response.getString("refresh_token"));
            user.setWhatsappNumber(phoneNumber);
            user.setCreatedAt(System.currentTimeMillis());
            user.setUpdatedAt(System.currentTimeMillis());
            user.setActive(true);
            user.setTokenExpiry(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000));
            userBox.put(user);
            // deactivate others
            for (User other : userBox.getAll()) {
                if (other.getId() != user.getId()) {
                    other.setActive(false);
                    userBox.put(other);
                }
            }
        } catch (JSONException ignored) {}
    }

    private String parseError(com.android.volley.VolleyError error) {
        String msg = "Unknown error";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String data = new String(error.networkResponse.data);
            try {
                JSONObject j = new JSONObject(data);
                if (j.has("message")) msg = j.getString("message");
                else if (j.has("error")) msg = j.getString("error");
                else msg = "Error " + error.networkResponse.statusCode;
            } catch (JSONException e) {
                msg = "Error " + error.networkResponse.statusCode;
            }
        } else if (error.getMessage() != null) {
            msg = error.getMessage();
        }
        return msg;
    }

    public enum AuthState {
        NOT_AUTHENTICATED,
        OTP_SENT,
        AUTHENTICATED
    }
}
