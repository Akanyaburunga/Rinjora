package rw.martinhardware.mymartin.ui.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.Locale;

import io.objectbox.Box;
import rw.martinhardware.mymartin.MyApp;
import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.auth.AuthActivity;
import rw.martinhardware.mymartin.data.DriverHomeRepository;
import rw.martinhardware.mymartin.databinding.FragmentHomeBinding;
import rw.martinhardware.mymartin.entities.HomeSnapshot;
import rw.martinhardware.mymartin.entities.User;
import rw.martinhardware.mymartin.entities.User_;
import rw.martinhardware.mymartin.util.AnalyticsHelper;
import rw.martinhardware.mymartin.util.DateUtils;

/**
 * Offline-first driver home.
 *
 * Renders from the cached {@link HomeSnapshot} instantly (works fully offline), then refreshes
 * from GET /api/mobile/trips/current in the background: a 60s poll while the screen is visible
 * plus a WorkManager periodic sync scheduled in MyApp.
 */
public class HomeFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 60_000L;
    private static final double LOW_FUEL_THRESHOLD_PERCENT = 15.0;

    private FragmentHomeBinding binding;
    private final DriverHomeRepository repository = new DriverHomeRepository();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            fetch(false);
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRefresh.setOnClickListener(v -> fetch(true));
        binding.btnRetry.setOnClickListener(v -> fetch(true));
        binding.btnDirections.setOnClickListener(v -> openDirections());
        binding.btnCallStaff.setOnClickListener(v -> callStaff());
        binding.btnWhatsappStaff.setOnClickListener(v -> whatsappStaff());

        binding.cardSupport.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.navigation_support));
        binding.cardProfile.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.navigation_profile));

        boolean hasCache = repository.getCached(requireContext()) != null;
        renderCached();
        fetch(!hasCache);
    }

    @Override
    public void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void renderCached() {
        HomeSnapshot cached = repository.getCached(requireContext());
        if (cached == null) return;
        bind(cached, !cached.isHasActiveTrip());
    }

    private void fetch(boolean showLoading) {
        if (binding == null) return;
        if (showLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.errorState.setVisibility(View.GONE);
        }
        binding.tvSyncStatus.setText("Updating...");

        repository.fetch(requireContext(), new DriverHomeRepository.Callback() {
            @Override
            public void onSuccess(HomeSnapshot snapshot, boolean idle) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.errorState.setVisibility(View.GONE);
                bind(snapshot, idle);
                binding.tvSyncStatus.setText("Updated " + elapsed(snapshot.getFetchedAt()));
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
                HomeSnapshot cached = repository.getCached(requireContext());
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

    private void bind(HomeSnapshot s, boolean idle) {
        binding.tvHello.setText("Welcome, " + driverName(s));

        if (idle) {
            binding.tvHeroSubtitle.setText("No active trip right now.");
        } else if (s.getTripReference() != null) {
            binding.tvHeroSubtitle.setText("Trip " + s.getTripReference());
        } else {
            binding.tvHeroSubtitle.setText("Here's what's happening with your deliveries.");
        }

        String plate = s.getPlateNumber();
        if (plate != null && !plate.isEmpty()) {
            binding.tvPlate.setText(plate);
            binding.tvPlate.setVisibility(View.VISIBLE);
        } else {
            binding.tvPlate.setVisibility(View.GONE);
        }

        // Current trip / idle
        binding.cardTrip.setVisibility(idle ? View.GONE : View.VISIBLE);
        binding.cardIdle.setVisibility(idle ? View.VISIBLE : View.GONE);
        if (!idle) {
            binding.tvTripStatus.setText(formatStatus(s.getTripStatus()));
            binding.tvTripRef.setText(s.getTripReference() != null ? "Ref: " + s.getTripReference() : "Active trip");

            String origin = s.getOrderOrigin() != null ? s.getOrderOrigin() : "Origin";
            String destination = s.getOrderDestination() != null ? s.getOrderDestination() : "Destination";
            binding.tvOriginDestination.setText(origin + " \u2192 " + destination);

            if (s.getRouteName() != null || s.getRouteDistanceKm() > 0) {
                StringBuilder route = new StringBuilder();
                if (s.getRouteName() != null) route.append(s.getRouteName());
                if (s.getRouteDistanceKm() > 0) {
                    if (route.length() > 0) route.append(" \u00b7 ");
                    route.append(formatKm(s.getRouteDistanceKm()));
                }
                binding.tvRoute.setText(route.toString());
                binding.tvRoute.setVisibility(View.VISIBLE);
            } else {
                binding.tvRoute.setVisibility(View.GONE);
            }
        }

        // Position / nearest place
        boolean hasPlace = s.getPlaceName() != null;
        binding.cardPosition.setVisibility(hasPlace ? View.VISIBLE : View.GONE);
        if (hasPlace) {
            binding.tvPlaceName.setText(s.getPlaceName());
            if (s.getPlaceDistanceMeters() > 0) {
                binding.tvPlaceDistance.setText(formatDistance(s.getPlaceDistanceMeters()) + " away");
            } else if (s.getPlaceCity() != null) {
                binding.tvPlaceDistance.setText(s.getPlaceCity());
            } else {
                binding.tvPlaceDistance.setText("Nearby");
            }
            if (s.isStale() && s.getLastSeenAt() != null) {
                binding.tvLastSeen.setVisibility(View.VISIBLE);
                binding.tvLastSeen.setText("Signal lost \u00b7 last seen " + DateUtils.relative(s.getLastSeenAt()));
            } else {
                binding.tvLastSeen.setVisibility(View.GONE);
            }
        }

        // Truck status summary
        boolean hasTruckStatus = s.isHasPosition() || s.getTankCapacity() > 0
                || s.getLastSeenAt() != null || s.isStale();
        binding.cardTruckStatus.setVisibility(hasTruckStatus ? View.VISIBLE : View.GONE);
        if (hasTruckStatus) {
            double level = Math.max(0, s.getFuelLevel());
            double capacity = s.getTankCapacity();
            int percent = (int) Math.min(100, Math.round(level / Math.max(1, capacity) * 100.0));

            binding.tvFuelValue.setText(formatLitres(level));

            boolean engineOn = s.isIgnition();
            binding.tvEngineValue.setText(engineOn ? "On" : "Off");
            binding.tvEngineValue.setTextColor(engineOn
                    ? ContextCompat.getColor(requireContext(), R.color.brand_success)
                    : ContextCompat.getColor(requireContext(), R.color.text_muted));
            binding.imgEngine.setImageTintList(engineOn
                    ? ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.brand_success))
                    : ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_muted)));

            binding.tvSpeedValue.setText(String.format(Locale.US, "%.0f km/h", s.getSpeed()));

            boolean stale = s.isStale();
            binding.tvSignalValue.setText(stale ? "Stale" : "Live");
            int signalColor = ContextCompat.getColor(requireContext(),
                    stale ? R.color.status_pending : R.color.brand_success);
            binding.tvSignalValue.setTextColor(signalColor);
            binding.tvTruckSignal.setText(stale ? "Stale" : "Live");
            binding.tvTruckSignal.setTextColor(signalColor);

            boolean showFuelBar = capacity > 0;
            binding.progressBarFuel.setVisibility(showFuelBar ? View.VISIBLE : View.GONE);
            binding.tvFuelDetail.setVisibility(showFuelBar ? View.VISIBLE : View.GONE);
            if (showFuelBar) {
                binding.progressBarFuel.setProgress(percent);
                binding.progressBarFuel.setProgressTintList(percent < LOW_FUEL_THRESHOLD_PERCENT
                        ? ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.brand_error))
                        : null);
                StringBuilder detail = new StringBuilder(formatLitres(level) + " of " + formatLitres(capacity));
                if (s.getFuelType() != null && !s.getFuelType().isEmpty()) detail.append(" \u00b7 ").append(s.getFuelType());
                binding.tvFuelDetail.setText(detail.toString());
            }
            binding.tvFuelWarning.setVisibility(showFuelBar && percent < LOW_FUEL_THRESHOLD_PERCENT
                    ? View.VISIBLE : View.GONE);
        }

        // Dispatcher
        boolean hasStaff = s.getStaffName() != null;
        binding.cardStaff.setVisibility(hasStaff ? View.VISIBLE : View.GONE);
        if (hasStaff) {
            binding.tvStaffName.setText(s.getStaffName());
            binding.tvStaffRoles.setText(s.getStaffRoles() != null ? s.getStaffRoles() : "Dispatcher");
            binding.btnCallStaff.setEnabled(s.getStaffPhone() != null);
            boolean hasWhatsapp = s.getStaffWhatsapp() != null && !digitsOnly(s.getStaffWhatsapp()).isEmpty();
            binding.btnWhatsappStaff.setVisibility(hasWhatsapp ? View.VISIBLE : View.GONE);
        }
    }

    private String driverName(HomeSnapshot s) {
        if (s != null && s.getDriverName() != null && !s.getDriverName().isEmpty()) return s.getDriverName();
        try {
            Box<User> box = ((MyApp) requireActivity().getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            if (u != null && u.getName() != null) return u.getName();
        } catch (Exception ignored) {}
        return "Driver";
    }

    private void openDirections() {
        HomeSnapshot s = repository.getCached(requireContext());
        if (s == null || s.getPlaceLat() == 0 && s.getPlaceLng() == 0) return;
        String label = s.getPlaceName() != null ? Uri.encode(s.getPlaceName()) : "";
        Uri geo = Uri.parse("geo:" + s.getPlaceLat() + "," + s.getPlaceLng() + "?q=" + s.getPlaceLat() + "," + s.getPlaceLng() + "(" + label + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (Exception ignored) {}
    }

    private void callStaff() {
        HomeSnapshot s = repository.getCached(requireContext());
        if (s == null || s.getStaffPhone() == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(s.getStaffPhone()))));
        } catch (Exception ignored) {}
    }

    private void whatsappStaff() {
        HomeSnapshot s = repository.getCached(requireContext());
        if (s == null || s.getStaffWhatsapp() == null) return;
        String digits = digitsOnly(s.getStaffWhatsapp());
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
        AnalyticsHelper.logEvent(requireContext(), "home_auth_error");
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) return "Active";
        String[] parts = status.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : "Active";
    }

    private String formatDistance(int meters) {
        if (meters < 1000) return meters + " m";
        return String.format(Locale.US, "%.1f km", meters / 1000.0);
    }

    private String formatKm(double km) {
        return String.format(Locale.US, "%.1f km", km);
    }

    private String formatLitres(double litres) {
        if (litres == Math.floor(litres)) {
            return String.format(Locale.US, "%d L", (long) litres);
        }
        return String.format(Locale.US, "%.1f L", litres);
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("[^0-9]", "");
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
        pollHandler.removeCallbacks(pollRunnable);
        binding = null;
    }
}
