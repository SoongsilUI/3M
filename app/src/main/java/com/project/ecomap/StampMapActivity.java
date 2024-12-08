package com.project.ecomap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.ecomap.databinding.ActivityStampMapBinding;

import java.util.ArrayList;
import java.util.List;

public class StampMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private final int FINE_PERMISSION_CODE = 1;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStampMapBinding binding = ActivityStampMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent로 전달된 데이터 받기
        String routeCode = getIntent().getStringExtra("ROUTE_CODE");

        // 지도 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.eventmap_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Firebase 데이터를 가져와 지도에 표시
        if (routeCode != null) {
            fetchMarkersAndDrawRoute(routeCode);
        }

        // 위치 서비스 설정
        setupLocationServices();

        Button exitButton = binding.exitButton;
        exitButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void fetchMarkersAndDrawRoute(String routeCode) {
        // Firestore 초기화
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore 경로: trails/{routeCode}/{trailCode}
        db.collection("trails")
                .document(routeCode)
                .collection("trails")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<LatLng> markerPoints = new ArrayList<>();

                        // Firestore 문서 순회
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");

                            if (latitude != null && longitude != null) {
                                LatLng point = new LatLng(latitude, longitude);
                                markerPoints.add(point);

                                // 지도에 마커 추가
                                myMap.addMarker(new MarkerOptions()
                                        .position(point)
                                        .title(document.getId())); // 문서 ID를 마커 이름으로 사용
                            }
                        }

                        // 마커 연결하여 빨간 선으로 경로 그리기
                        drawPolyline(markerPoints);
                    } else {
                        Toast.makeText(StampMapActivity.this, "트레일 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StampMapActivity.this, "데이터 로드 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void drawPolyline(List<LatLng> points) {
        if (myMap != null && !points.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.RED)
                    .width(8);

            myMap.addPolyline(polylineOptions);

            // 경로를 화면 중앙에 맞춤
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                boundsBuilder.include(point);
            }
            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        }
    }

    private void setupLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateCurrentLocation(location);
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateCurrentLocation(location);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);

            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            });
        }

        myMap.setOnMapClickListener(latLng -> {
            myMap.addMarker(new MarkerOptions().position(latLng).title("새로운 위치"));
            Toast.makeText(this, "마커 추가: " + latLng.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    public void updateCurrentLocation(@NonNull Location location) {
        if (myMap != null) {
            currentLocation = location;

            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));

            Log.d("LocationUpdate", "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}