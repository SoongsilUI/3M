package com.project.ecomap;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;


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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ecomap.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Uri selectedImageUri;
    private boolean isFabOpen = false;

    private LocationCallback locationCallback;

    FirebaseAuth auth;

    private boolean isCameraFollowing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Map Fragment 설정
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.ecomap_mapFragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // 초기 설정 메서드 호출
        setupImagePickerLauncher();
        setupFabButtons();
        setupLocationServices();
        setupToolbarAndDrawer();

        // 이미지 뷰 초기 상태 (이미지 없음)
        binding.ecomapSelectedImageView.setVisibility(View.GONE);

        // 버튼 클릭 시 현재 위치로 이동
        binding.currentLocationButton.setOnClickListener(v -> {
            if (currentLocation != null) {
                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                isCameraFollowing = true;

                requestLocationUpdates();
            }

        });


    }

    // 이미지 Picker 초기화
    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            binding.ecomapSelectedImageView.setImageURI(selectedImageUri);
                            binding.ecomapSelectedImageView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }

    // Floating Action Button (FAB) 설정
    private void setupFabButtons() {
        binding.fabAddPhoto.setOnClickListener(view -> toggleFab());
        binding.fabUpload.setOnClickListener(view -> showInputDialog());
    }

    // 위치 서비스 설정 및 위치 업데이트 요청
    private void setupLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();
    }

    // Toolbar 및 Drawer 설정
    private void setupToolbarAndDrawer() {
        binding.ecomapTopAppBar.setNavigationOnClickListener(view -> binding.ecomapDrawerLayout.openDrawer(GravityCompat.START));
        binding.ecomapNavigationView.setVisibility(View.VISIBLE);
        binding.ecomapNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            binding.ecomapDrawerLayout.closeDrawer(GravityCompat.START);

            String message = "";
            Intent intent = null;

            if (id == R.id.navigationItems_photoShare) {
                message = "사진을 공유해주세요";
            } else if (id == R.id.navigationItems_askQuestion) {
                message = "질문하기";

                intent = new Intent(MainActivity.this, CreateNewQuestionActivity.class);
                startActivity(intent);
            } else if (id == R.id.navigationItems_queryBoard) {
                message = "질문과 답변을 공유해주세요";

                intent = new Intent(MainActivity.this, QuestionListPreviewActivity.class);
                startActivity(intent);
            } else if (id == R.id.navigationItems_photoBoard) {
                message = "사진을 구경하세요";
            } else if (id == R.id.navigationItems_settings) {
                message = "회원탈퇴/알림설정";
            } else if (id == R.id.navigationItems_stampMission) {
                message = "오픈 준비중";
            }

            if (!message.isEmpty()) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            return true;
        });

    }

    // FAB Toggle (펼치기/접기)
    private void toggleFab() {
        if (isFabOpen) {
            binding.fabUpload.setVisibility(View.GONE);
            binding.fabQuery.setVisibility(View.GONE);
        } else {
            binding.fabUpload.setVisibility(View.VISIBLE);
            binding.fabQuery.setVisibility(View.VISIBLE);
        }
        isFabOpen = !isFabOpen;
    }

    // 위치 업데이트 요청
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // 마지막 위치 가져와 초기 위치 설정
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                if (isCameraFollowing) {
                    updateCurrentLocation(location);
                    runOnUiThread(() -> {
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.ecomap_mapFragment);
                        if (mapFragment != null) {
                            mapFragment.getMapAsync(MainActivity.this);
                        }
                    });
                }
            }
        });
    }

    // 입력 Dialog 표시
    private void showInputDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_input);

        EditText inputEditText = dialog.findViewById(R.id.edit_marker_title);
        Button selectImageButton = dialog.findViewById(R.id.btn_select_image);
        Button registerButton = dialog.findViewById(R.id.btn_register_marker);
        ImageButton closeButton = dialog.findViewById(R.id.btn_close_dialog);

        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        selectImageButton.setOnClickListener(view -> openImagePicker());

        registerButton.setOnClickListener(v -> {
            String markerTitle = inputEditText.getText().toString();
            addMarkerAtCurrentLocation(markerTitle, selectedImageUri); // selectedImageUri는 이미지 선택 후 갱신된 URI
            dialog.dismiss();
        });

        dialog.show();
    }

    // 이미지 Picker 호출
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // 마커 추가
    private void addMarkerAtCurrentLocation(String markerTitle, Uri imageUri) {
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            MarkerOptions options = new MarkerOptions()
                    .position(currentLatLng)
                    .title(markerTitle);

            Marker marker = myMap.addMarker(options);

            assert marker != null;
            marker.setTag(imageUri);
        } else {
            Toast.makeText(this, "위치 찾을 수 없음", Toast.LENGTH_SHORT).show();
        }
    }

    // 마커 클릭 시 Dialog 표시
    private void showMarkerDialog(@NonNull Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String markerTitle = marker.getTitle(); // 마커의 제목 설정
        builder.setTitle(markerTitle);

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_marker, null);

        ImageView markerImageView = dialogView.findViewById(R.id.maker_image);
        Button confirmButton = dialogView.findViewById(R.id.btn_confirm);

        Uri imageUri = (Uri) marker.getTag();
        if (imageUri != null) {
            markerImageView.setImageURI(imageUri);
            markerImageView.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = builder.setView(dialogView).create();
        confirmButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

    // 위치 변경 이벤트 처리
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        updateCurrentLocation(location);
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

        // 지도 이동 event listener
        myMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isCameraFollowing = false;
            }
        });

        requestLocationUpdates();

        myMap.setOnMarkerClickListener(marker -> {
            showMarkerDialog(marker);
            return true;
        });
    }

    // 현재 위치를 지도에 표시
    public void updateCurrentLocation(@NonNull Location location) {
        if (myMap != null) {
            currentLocation = location;

            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            Log.d("aaa", "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
            Log.d("aaa", "Is Camera Following: " + isCameraFollowing);

            if (isCameraFollowing) {
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Activity 중지될 때 위치 업데이트 콜백 제거
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}

