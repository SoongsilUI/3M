package com.project.ecomap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
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
import com.project.ecomap.databinding.ActivityStampMapBinding;

import java.util.ArrayList;
import java.util.List;

public class StampMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private final int FINE_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStampMapBinding binding = ActivityStampMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent로 전달된 코드 받기
        String routeCode = getIntent().getStringExtra("ROUTE_CODE");

        // Firebase 경로 데이터를 받아와서 지도에 표시
        if (routeCode != null) {
            fetchRouteFromFirebase(routeCode);
        }

        // 지도 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.eventmap_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // 위치 서비스 설정
        setupLocationServices();
    }
    private void fetchRouteFromFirebase(String routeCode) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("routes").child(routeCode);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<LatLng> routePoints = new ArrayList<>();
                    for (DataSnapshot pointSnapshot : snapshot.getChildren()) {
                        double latitude = pointSnapshot.child("latitude").getValue(Double.class);
                        double longitude = pointSnapshot.child("longitude").getValue(Double.class);
                        routePoints.add(new LatLng(latitude, longitude));
                    }
                    drawRouteOnMap(routePoints);
                } else {
                    Toast.makeText(StampMapActivity.this, "경로 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StampMapActivity.this, "데이터 로드 중 오류 발생: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRouteOnMap(List<LatLng> routePoints) {
        if (myMap != null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.RED)  // 빨간색 경로
                    .width(8);         // 경로 두께
            myMap.addPolyline(polylineOptions);

            // 경로 중앙으로 카메라 이동
            if (!routePoints.isEmpty()) {
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                for (LatLng point : routePoints) {
                    boundsBuilder.include(point);
                }
                myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            }
        }
    }




    // 위치 서비스 설정
    private void setupLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();
    }

    // 위치 업데이트 요청
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        // 위치 콜백 설정
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

        // 마지막 위치 가져와 초기 위치 설정
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateCurrentLocation(location);
            }
        });
    }

    // Google Map 초기화 작업 처리
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);

            // 마지막 위치 가져오기
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            });
        }

        // 지도 클릭 시 마커 추가
        myMap.setOnMapClickListener(latLng -> {
            myMap.addMarker(new MarkerOptions().position(latLng).title("새로운 위치"));
            Toast.makeText(this, "마커 추가: " + latLng.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    // 현재 위치를 지도에 표시
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
        // Activity 중지될 때 위치 업데이트 콜백 제거
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
