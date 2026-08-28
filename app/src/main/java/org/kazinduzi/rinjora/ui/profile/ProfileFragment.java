package org.kazinduzi.rinjora.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Locale;

import io.objectbox.Box;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.auth.AuthActivity;
import org.kazinduzi.rinjora.data.DriverProfileRepository;
import org.kazinduzi.rinjora.databinding.FragmentProfileBinding;
import org.kazinduzi.rinjora.entities.DriverProfile;
import org.kazinduzi.rinjora.entities.DriverTrip;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.util.AnalyticsHelper;
import org.kazinduzi.rinjora.util.DateUtils;
import org.kazinduzi.rinjora.viewmodel.AuthViewModel;

/**
 * Offline-first driver profile.
 *
 * Renders from the cached {@link DriverProfile} instantly (works fully offline), then refreshes
 * from GET /api/mobile/profile in the background (WorkManager periodic sync + refresh on open).
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthViewModel authViewModel;
    private final DriverProfileRepository repository = new DriverProfileRepository();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.llLogout.setOnClickListener(v -> showLogoutConfirmation());
        binding.btnRefresh.setOnClickListener(v -> fetch(true));
        binding.btnRetry.setOnClickListener(v -> fetch(true));
        binding.llPhoneRow.setOnClickListener(v -> dialPhone());
        binding.llWhatsappRow.setOnClickListener(v -> openWhatsapp());

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            if (state == AuthViewModel.AuthState.NOT_AUTHENTICATED) {
                Intent intent = new Intent(getContext(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        boolean hasCache = repository.getCached(requireContext()) != null;
        renderCached();
        fetch(!hasCache);
    }

    private void renderCached() {
        DriverProfile cached = repository.getCached(requireContext());
        if (cached == null) return;
        bind(cached);
    }

    private void fetch(boolean showLoading) {
        if (binding == null) return;
        if (showLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.errorState.setVisibility(View.GONE);
        }
        binding.tvSyncStatus.setText("Updating...");

        repository.fetch(requireContext(), new DriverProfileRepository.Callback() {
            @Override
            public void onSuccess(DriverProfile profile) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.errorState.setVisibility(View.GONE);
                bind(profile);
                binding.tvSyncStatus.setText("Updated " + elapsed(profile.getFetchedAt()));
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                handleAuthError();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                DriverProfile cached = repository.getCached(requireContext());
                if (cached != null) {
                    binding.tvSyncStatus.setText("Offline \u00b7 last update " + elapsed(cached.getFetchedAt()));
                    Toast.makeText(getContext(), "Offline \u2014 showing last update", Toast.LENGTH_SHORT).show();
                } else {
                    binding.errorState.setVisibility(View.VISIBLE);
                    binding.tvError.setText(message);
                }
            }
        });
    }

    private void bind(DriverProfile p) {
        binding.tvDriverName.setText(p.getDriverName() != null ? p.getDriverName() : fallbackName());

        String idText = getLocalUuid();
        if (idText == null && p.getDriverEmail() != null) idText = p.getDriverEmail();
        binding.tvDriverId.setText(idText != null ? "ID: " + idText : "");

        if (p.getRating() > 0) {
            binding.tvRating.setText("\u2605 " + String.format(Locale.US, "%.1f", p.getRating())
                    + " (" + p.getRatingCount() + " ratings)");
            binding.tvRating.setVisibility(View.VISIBLE);
        } else {
            binding.tvRating.setVisibility(View.GONE);
        }

        if (p.getMemberSince() != null && !p.getMemberSince().isEmpty()) {
            binding.tvMemberSince.setText("Member since " + DateUtils.date(p.getMemberSince()));
            binding.tvMemberSince.setVisibility(View.VISIBLE);
        } else {
            binding.tvMemberSince.setVisibility(View.GONE);
        }

        binding.tvPhoneNumber.setText(p.getDriverPhone() != null ? p.getDriverPhone() : "N/A");
        binding.tvWhatsappNumber.setText(p.getDriverWhatsapp() != null ? p.getDriverWhatsapp() : "N/A");
        binding.tvEmail.setText(p.getDriverEmail() != null ? p.getDriverEmail() : "N/A");
        binding.tvNationality.setText(p.getDriverNationality() != null ? p.getDriverNationality() : "N/A");
        binding.tvBranch.setText(p.getDriverBranch() != null ? p.getDriverBranch() : "N/A");
        binding.tvVehicleNumber.setText(p.getPlateNumber() != null ? p.getPlateNumber() : "N/A");
        binding.tvTrailerNumber.setText(p.getTrailerPlate() != null ? p.getTrailerPlate() : "None");

        binding.llPhoneRow.setEnabled(p.getDriverPhone() != null);
        binding.llWhatsappRow.setEnabled(p.getDriverWhatsapp() != null
                && !digitsOnly(p.getDriverWhatsapp()).isEmpty());

        binding.cardDeliveryStats.setVisibility(View.VISIBLE);
        binding.tvTotalDeliveries.setText(String.valueOf(p.getTotalTrips()));
        binding.tvCompletedDeliveries.setText(String.valueOf(p.getCompletedTrips()));
        binding.tvPendingDeliveries.setText(String.valueOf(p.getPendingTrips()));
        if (p.getTotalDistanceKm() > 0 || p.getHoursDriven() > 0) {
            binding.tvStatsExtra.setText(formatKm(p.getTotalDistanceKm()) + " total \u00b7 "
                    + formatHours(p.getHoursDriven()) + " driven");
        } else {
            binding.tvStatsExtra.setText("");
        }

        bindLatestTrips(p.getDriverId());
    }

    private void bindLatestTrips(long driverId) {
        List<DriverTrip> trips = repository.getCachedTrips(requireContext(), driverId);
        binding.llLatestTrips.removeAllViews();
        if (trips == null || trips.isEmpty()) {
            binding.tvNoTrips.setVisibility(View.VISIBLE);
            binding.cardLatestTrips.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoTrips.setVisibility(View.GONE);
        binding.cardLatestTrips.setVisibility(View.VISIBLE);

        for (int i = 0; i < trips.size(); i++) {
            DriverTrip t = trips.get(i);
            binding.llLatestTrips.addView(buildTripRow(t, i < trips.size() - 1));
        }
    }

    private View buildTripRow(DriverTrip t, boolean withDivider) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 10, 0, 10);

        LinearLayout left = new LinearLayout(requireContext());
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView ref = new TextView(requireContext());
        ref.setText(t.getReference() != null ? t.getReference() : "Trip");
        ref.setTextSize(14);
        ref.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        left.addView(ref);

        TextView route = new TextView(requireContext());
        String origin = t.getOrigin() != null ? t.getOrigin() : "Origin";
        String destination = t.getDestination() != null ? t.getDestination() : "Destination";
        route.setText(origin + " \u2192 " + destination);
        route.setTextSize(12);
        route.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        left.addView(route);

        row.addView(left);

        LinearLayout right = new LinearLayout(requireContext());
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(android.view.Gravity.END);

        TextView status = new TextView(requireContext());
        status.setText(formatStatus(t.getStatus()));
        status.setTextSize(13);
        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        status.setPadding(dp(8), dp(2), dp(8), dp(2));
        status.setBackgroundResource(R.drawable.bg_status_chip);
        right.addView(status);

        if (t.getEndedAt() != null && !t.getEndedAt().isEmpty()) {
            TextView ended = new TextView(requireContext());
            ended.setText(DateUtils.date(t.getEndedAt()));
            ended.setTextSize(12);
            ended.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            right.addView(ended);
        }

        row.addView(right);

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        if (withDivider) {
            View line = new View(requireContext());
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_outline_variant));
            wrapper.addView(line);
        }
        return wrapper;
    }

    private void dialPhone() {
        DriverProfile p = repository.getCached(requireContext());
        if (p == null || p.getDriverPhone() == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(p.getDriverPhone()))));
        } catch (Exception ignored) {}
    }

    private void openWhatsapp() {
        DriverProfile p = repository.getCached(requireContext());
        if (p == null || p.getDriverWhatsapp() == null) return;
        String digits = digitsOnly(p.getDriverWhatsapp());
        if (digits.isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits)));
        } catch (Exception ignored) {}
    }

    private void handleAuthError() {
        try {
            Box<User> box = ((MyApp) requireActivity().getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            if (u != null) {
                u.setActive(false);
                u.setToken(null);
                u.setRefreshToken(null);
                box.put(u);
            }
        } catch (Exception ignored) {}
        AnalyticsHelper.logEvent(requireContext(), "profile_auth_error");
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private String fallbackName() {
        try {
            Box<User> box = ((MyApp) requireActivity().getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            if (u != null && u.getName() != null) return u.getName();
        } catch (Exception ignored) {}
        return "Driver";
    }

    private String getLocalUuid() {
        try {
            Box<User> box = ((MyApp) requireActivity().getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            if (u != null) return u.getUuid();
        } catch (Exception ignored) {}
        return null;
    }

    private void showLogoutConfirmation() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        AnalyticsHelper.logEvent(requireContext(), "logout");
        authViewModel.logout();
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) return "Trip";
        String[] parts = status.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : "Trip";
    }

    private String formatKm(double km) {
        if (km == Math.floor(km)) return String.format(Locale.US, "%,d km", (long) km);
        return String.format(Locale.US, "%,.1f km", km);
    }

    private String formatHours(double hours) {
        return String.format(Locale.US, "%,.0f hrs", hours);
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("[^0-9]", "");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String elapsed(long epochMs) {
        long diff = System.currentTimeMillis() - epochMs;
        if (diff < 60_000) return "just now";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        return (diff / 86_400_000) + "d ago";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
