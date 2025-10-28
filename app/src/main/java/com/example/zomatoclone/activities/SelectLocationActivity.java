package com.example.zomatoclone.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.zomatoclone.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;              // correct import
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SelectLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private FusedLocationProviderClient fusedLocationClient;
    private EditText nameField, phoneField;
    private Button btnSave;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        nameField = findViewById(R.id.nameField);
        phoneField = findViewById(R.id.phoneField);
        btnSave = findViewById(R.id.btnSaveAddress);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSave.setOnClickListener(v -> saveSelectedAddress());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission then return — when user grants, onRequestPermissionsResult will not re-run getMap automatically,
            // but user can press detect again or you can handle it more gracefully.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Enable "my location" blue dot
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        // Try to get last known location and move camera there
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                selectedLatLng = currentLatLng;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));
            }
        }).addOnFailureListener(e -> {
            // ignore - user may not have location or service failed
        });

        // User can tap the map to select another location
        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        });
    }

    private void saveSelectedAddress() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = nameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reverse geocode to get human readable address
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(selectedLatLng.latitude, selectedLatLng.longitude, 1);
            String addressLine = (addresses != null && !addresses.isEmpty()) ? addresses.get(0).getAddressLine(0) : "";
            String fullAddress = name + ", " + phone + (addressLine.isEmpty() ? "" : ", " + addressLine);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("address", fullAddress);
            resultIntent.putExtra("latitude", selectedLatLng.latitude);
            resultIntent.putExtra("longitude", selectedLatLng.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // handle permission result if user grants/denies
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted — try to re-initialize map location features
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            selectedLatLng = currentLatLng;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Location permission is required to detect position", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
