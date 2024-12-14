package com.project.ecomap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView progressText;
    private int totalMarkers = 0; // 전체 마커 개수
    private int visitedMarkers = 0; // 방문한 마커 개수
    private List<LatLng> markerPoints = new ArrayList<>(); // 마커 위치 저장
    private static final float VISIT_RADIUS = 50; // 방문 거리 기준 (단위: 미터)
    private static final String TAG = "StampMapActivity_log"; // 로그 태그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStampMapBinding binding = ActivityStampMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "StampMapActivity 시작됨");

        // ProgressBar와 TextView 초기화
        progressBar = binding.progressBar;
        progressText = binding.tvProgress;

        // Intent로 전달된 데이터 받기
        String routeCode = getIntent().getStringExtra("ROUTE_CODE");
        Log.d(TAG, "전달된 routeCode = " + routeCode);

        // 지도 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.eventmap_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Firebase 데이터를 가져와 지도에 표시
        if (routeCode != null) {
            fetchMarkersAndDrawRoute(routeCode);
        } else {
            Log.w(TAG, "routeCode가 null입니다.");
        }

        // 위치 서비스 설정
        setupLocationServices();

        Button exitButton = binding.exitButton;
        exitButton.setOnClickListener(v -> {
            Log.d(TAG, "종료 버튼 클릭됨");
            finish();
        });
    }

    private void fetchMarkersAndDrawRoute(String routeCode) {
        Log.d(TAG, "Firebase에서 마커 데이터 가져오기 시작");
        db = FirebaseFirestore.getInstance();

        db.collection("trails")
                .document(routeCode)
                .collection("trails")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");

                            if (latitude != null && longitude != null) {
                                LatLng point = new LatLng(latitude, longitude);
                                markerPoints.add(point);
                                Log.d(TAG, "마커 추가됨 - " + point);

                                // 지도에 마커 추가
                                myMap.addMarker(new MarkerOptions()
                                        .position(point)
                                        .title(document.getId())); // 문서 ID를 마커 이름으로 사용
                            }
                        }

                        // 마커 개수 설정 및 ProgressBar 업데이트
                        totalMarkers = markerPoints.size();
                        updateProgressBar();

                        // 마커 연결하여 빨간 선으로 경로 그리기
                        drawPolyline(markerPoints);
                    } else {
                        Log.e(TAG, "Firebase 데이터 가져오기 실패", task.getException());
                        Toast.makeText(StampMapActivity.this, "트레일 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase 데이터 로드 중 오류", e);
                    Toast.makeText(StampMapActivity.this, "데이터 로드 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void drawPolyline(List<LatLng> points) {
        if (myMap != null && !points.isEmpty()) {
            Log.d(TAG, "경로 그리기 시작");
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.RED)
                    .width(8);

            myMap.addPolyline(polylineOptions);

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                boundsBuilder.include(point);
            }
            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            Log.d(TAG, "경로 그리기 완료");
        } else {
            Log.w(TAG, "경로를 그릴 데이터가 없습니다.");
        }
    }

    private void setupLocationServices() {
        Log.d(TAG, "위치 서비스 설정 시작");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        Log.d(TAG, "위치 업데이트 요청");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "위치 권한이 없음");
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

    public void updateCurrentLocation(@NonNull Location location) {
        currentLocation = location;
        Log.d(TAG, "위치 업데이트됨 - " + location.getLatitude() + ", " + location.getLongitude());

        // 방문한 마커를 확인
        checkVisitedMarkers();
    }

    private void checkVisitedMarkers() {
        if (currentLocation != null) {
            Log.d(TAG, "방문한 마커 확인 시작");
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            for (LatLng marker : markerPoints) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLatLng.latitude, currentLatLng.longitude,
                        marker.latitude, marker.longitude,
                        results);

                // 방문한 마커는 ProgressBar에 반영
                if (results[0] < VISIT_RADIUS) {
                    visitedMarkers++;
                    Log.d(TAG, "마커 방문 확인됨" + marker);
                    updateProgressBar();
                    break;
                }
            }
        }
    }

    private void updateProgressBar() {
        Log.d(TAG, "ProgressBar 업데이트 시작");
        progressBar.setMax(totalMarkers);
        progressBar.setProgress(visitedMarkers);
        progressText.setText(visitedMarkers + "/" + totalMarkers);
        Log.d(TAG, "ProgressBar 업데이트 완료");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "위치 업데이트 중지");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "위치 권한 승인됨");
                requestLocationUpdates();
            } else {
                Log.w(TAG, "위치 권한이 거부됨");
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "지도 준비 완료");
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);

            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    Log.d(TAG, "초기 위치 설정");
                }
            });
        } else {
            Log.w(TAG, "위치 권한 없음");
        }
    }
}
