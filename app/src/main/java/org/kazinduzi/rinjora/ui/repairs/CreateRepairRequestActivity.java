package org.kazinduzi.rinjora.ui.repairs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityCreateRepairRequestBinding;
import org.kazinduzi.rinjora.databinding.ItemAddedPhotoBinding;
import org.kazinduzi.rinjora.databinding.ItemLineItemBinding;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.RepairItem;
import org.kazinduzi.rinjora.models.RepairVehicle;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;
import org.kazinduzi.rinjora.util.AnalyticsHelper;

public class CreateRepairRequestActivity extends BaseActivity {

    private static final int REQ_LOCATION = 100;
    private static final int REQ_CAMERA_PERM = 101;
    private static final int REQ_IMAGE_CAPTURE = 102;

    private ActivityCreateRepairRequestBinding binding;
    private VolleySingleton volley;
    private String token;

    private final List<RepairVehicle> vehicles = new ArrayList<>();
    private final List<View> itemRows = new ArrayList<>();
    private final Map<View, RepairItem.Part> rowParts = new HashMap<>();
    private final List<Uri> photoUris = new ArrayList<>();
    private Uri pendingPhotoUri;
    private boolean vehiclesLoaded;
    private boolean locationSubmitted;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private Runnable locationTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateRepairRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        volley = VolleySingleton.getInstance(this);
        token = getAuthToken();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"mechanical", "electrical", "body", "tire", "brake", "other"}));
        binding.spinnerPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"low", "medium", "high", "critical"}));

        binding.btnAddItem.setOnClickListener(v -> addItemRow());
        binding.btnAddPhoto.setOnClickListener(v -> requestCamera());
        binding.btnSubmit.setOnClickListener(v -> onSubmitClicked());

        addItemRow();
        loadVehicles();
    }

    private void loadVehicles() {
        binding.btnSubmit.setEnabled(false);
        vehiclesLoaded = false;
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, ApiConfig.REPAIR_VEHICLES, null,
                response -> {
                    vehicles.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject j = response.getJSONObject(i);
                            RepairVehicle v = new RepairVehicle();
                            v.setId(j.optInt("id"));
                            v.setPlateNumber(j.optString("plate_number"));
                            v.setStatus(j.optString("status"));
                            vehicles.add(v);
                        } catch (JSONException ignored) {}
                    }
                    vehiclesLoaded = true;
                    populateVehicleSpinner();
                },
                error -> {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed to load vehicles. Try again.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    private void populateVehicleSpinner() {
        List<String> labels = new ArrayList<>();
        for (RepairVehicle v : vehicles) {
            labels.add(v.getPlateNumber() + "  (" + v.getStatus() + ")");
        }
        if (labels.isEmpty()) {
            labels.add("No vehicles assigned");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        binding.spinnerVehicle.setAdapter(adapter);
        binding.btnSubmit.setEnabled(true);
    }

    private void addItemRow() {
        ItemLineItemBinding rowBinding = ItemLineItemBinding.inflate(getLayoutInflater());
        rowBinding.btnRemoveItem.setOnClickListener(v -> {
            itemRows.remove(rowBinding.getRoot());
            rowParts.remove(rowBinding.getRoot());
            binding.llItems.removeView(rowBinding.getRoot());
        });
        rowBinding.btnPickPart.setOnClickListener(v -> pickPart(rowBinding));
        itemRows.add(rowBinding.getRoot());
        binding.llItems.addView(rowBinding.getRoot());
        binding.tvItemsHint.setVisibility(View.GONE);
    }

    private void pickPart(ItemLineItemBinding rowBinding) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 8);

        com.google.android.material.textfield.TextInputEditText search =
                new com.google.android.material.textfield.TextInputEditText(this);
        search.setHint("Search parts");
        search.setSingleLine(true);
        layout.addView(search);

        ProgressBar progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        layout.addView(progress);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        layout.addView(list);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Attach part")
                .setView(layout)
                .setNeutralButton("Clear part", (d, w) -> {
                    rowParts.remove(rowBinding.getRoot());
                    rowBinding.tvSelectedPart.setText("No part selected");
                    rowBinding.tvSelectedPart.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
                })
                .setNegativeButton("Cancel", null)
                .create();

        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(listAdapter);

        final ArrayList<RepairItem.Part> results = new ArrayList<>();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                results.clear();
                listAdapter.clear();
                volley.getRequestQueue().cancelAll("parts_search");
                if (q.isEmpty()) {
                    progress.setVisibility(View.GONE);
                    return;
                }
                progress.setVisibility(View.VISIBLE);
                JsonArrayRequest req = new JsonArrayRequest(
                        Request.Method.GET, ApiConfig.repairParts(q), null,
                        response -> {
                            progress.setVisibility(View.GONE);
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    JSONObject j = response.getJSONObject(i);
                                    RepairItem.Part part = new RepairItem.Part();
                                    part.setId(j.optInt("id"));
                                    part.setName(j.optString("name"));
                                    part.setSku(j.optString("sku"));
                                    part.setUnitPrice(j.optDouble("unit_price", 0));
                                    part.setUnitOfMeasure(j.optString("unit_of_measure"));
                                    results.add(part);
                                    String label = part.getName();
                                    if (part.getSku() != null && !part.getSku().isEmpty()) label += " (" + part.getSku() + ")";
                                    listAdapter.add(label);
                                } catch (JSONException ignored) {}
                            }
                        },
                        error -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(CreateRepairRequestActivity.this, "Failed to search parts", Toast.LENGTH_SHORT).show();
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> h = new HashMap<>();
                        h.put("Accept", "application/json");
                        if (token != null) h.put("Authorization", "Bearer " + token);
                        return h;
                    }
                };
                req.setTag("parts_search");
                volley.addToRequestQueue(req);
            }
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < results.size()) {
                RepairItem.Part part = results.get(position);
                rowParts.put(rowBinding.getRoot(), part);
                String label = part.getName();
                if (part.getSku() != null && !part.getSku().isEmpty()) label += " (" + part.getSku() + ")";
                rowBinding.tvSelectedPart.setText("Part: " + label);
                rowBinding.tvSelectedPart.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            return;
        }
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, pendingPhotoUri);
        startActivityForResult(takePicture, REQ_IMAGE_CAPTURE);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "repairs");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "RR_" + timeStamp + ".jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMAGE_CAPTURE && resultCode == RESULT_OK && pendingPhotoUri != null) {
            addPhotoView(pendingPhotoUri);
            pendingPhotoUri = null;
        }
    }

    private void addPhotoView(Uri uri) {
        ItemAddedPhotoBinding photoBinding = ItemAddedPhotoBinding.inflate(getLayoutInflater());
        photoBinding.ivPhoto.setImageURI(uri);
        photoBinding.btnRemovePhoto.setOnClickListener(v -> {
            photoUris.remove(uri);
            binding.llPhotos.removeView(photoBinding.getRoot());
        });
        photoUris.add(uri);
        binding.llPhotos.addView(photoBinding.getRoot());
        binding.tvPhotosHint.setVisibility(View.GONE);
    }

    private void onSubmitClicked() {
        if (!vehiclesLoaded) {
            Toast.makeText(this, "Loading vehicles\u2026 try again in a moment", Toast.LENGTH_SHORT).show();
            loadVehicles();
            return;
        }
        String description = binding.etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (vehicles.isEmpty() || binding.spinnerVehicle.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "No vehicle selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }
        getLocationAndSubmit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission needed to add photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSubmit();
            } else {
                Toast.makeText(this, "Location permission needed to submit from the yard", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndSubmit() {
        binding.btnSubmit.setEnabled(false);
        locationSubmitted = false;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location last = null;
        try {
            if (lm != null) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null && lm != null) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {}

        if (last != null) {
            submitRequest(last.getLatitude(), last.getLongitude());
            return;
        }

        if (lm == null) {
            submitRequest(Double.NaN, Double.NaN);
            return;
        }

        Toast.makeText(this, "Getting your location\u2026", Toast.LENGTH_SHORT).show();
        locationTimeout = () -> {
            if (!locationSubmitted) submitRequest(Double.NaN, Double.NaN);
        };
        locationHandler.postDelayed(locationTimeout, 12000);

        try {
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, location -> {
                if (!locationSubmitted) {
                    locationHandler.removeCallbacks(locationTimeout);
                    submitRequest(location != null ? location.getLatitude() : Double.NaN,
                            location != null ? location.getLongitude() : Double.NaN);
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            if (!locationSubmitted) {
                locationHandler.removeCallbacks(locationTimeout);
                submitRequest(Double.NaN, Double.NaN);
            }
        }
    }

    private void submitRequest(double latitude, double longitude) {
        locationSubmitted = true;
        if (locationTimeout != null) locationHandler.removeCallbacks(locationTimeout);
        JSONObject body = new JSONObject();
        try {
            RepairVehicle vehicle = vehicles.get(binding.spinnerVehicle.getSelectedItemPosition());
            body.put("vehicle_id", vehicle.getId());
            body.put("type", binding.spinnerType.getSelectedItem().toString());
            body.put("priority", binding.spinnerPriority.getSelectedItem().toString());
            body.put("description", binding.etDescription.getText().toString().trim());
            if (!Double.isNaN(latitude)) body.put("latitude", latitude);
            if (!Double.isNaN(longitude)) body.put("longitude", longitude);

            JSONArray items = new JSONArray();
            for (View row : itemRows) {
                ItemLineItemBinding rb = ItemLineItemBinding.bind(row);
                String desc = rb.etItemDesc.getText().toString().trim();
                if (desc.isEmpty()) continue;
                JSONObject item = new JSONObject();
                item.put("description", desc);
                RepairItem.Part part = rowParts.get(row);
                if (part != null) item.put("part_id", part.getId());
                double qty = parseDoubleSafe(rb.etItemQty.getText().toString(), 1.0);
                double price = parseDoubleSafe(rb.etItemPrice.getText().toString(), 0.0);
                item.put("estimated_quantity", qty);
                item.put("estimated_unit_price", price);
                items.put(item);
            }
            if (items.length() > 0) body.put("items", items);
        } catch (JSONException e) {
            Toast.makeText(this, "Could not build request", Toast.LENGTH_SHORT).show();
            binding.btnSubmit.setEnabled(true);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.REPAIR_REQUESTS, body,
                response -> {
                    Toast.makeText(this, "Repair request created", Toast.LENGTH_SHORT).show();
                    AnalyticsHelper.logEvent(this, "repair_request_created");
                    JSONObject data = response.optJSONObject("data");
                    int createdId = data != null ? data.optInt("id") : response.optInt("id");
                    Intent intent = new Intent(this, RepairRequestDetailActivity.class);
                    intent.putExtra("request_id", createdId);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    binding.btnSubmit.setEnabled(true);
                    String msg = "Failed to create repair request";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String json = new String(error.networkResponse.data, "UTF-8");
                            JSONObject err = new JSONObject(json);
                            if (err.has("message")) msg = err.getString("message");
                            if (err.has("errors")) {
                                JSONObject errors = err.getJSONObject("errors");
                                if (errors.length() > 0) {
                                    String key = errors.names().getString(0);
                                    JSONArray arr = errors.getJSONArray(key);
                                    if (arr.length() > 0) msg = arr.optString(0);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (error.networkResponse != null && error.networkResponse.statusCode == 422) {
                        showServerError("Not inside the yard?", msg);
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    private double parseDoubleSafe(String s, double fallback) {
        try {
            if (s == null || s.trim().isEmpty()) return fallback;
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void showServerError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getAuthToken() {
        try {
            Box<User> box = ((MyApp) getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            return u != null ? u.getToken() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
