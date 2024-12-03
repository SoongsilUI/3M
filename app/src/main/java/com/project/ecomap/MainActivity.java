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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;


import com.bumptech.glide.Glide;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.ecomap.Models.ProfileModel;
import com.project.ecomap.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

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

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String userId;
    private String username;
    FirebaseUser currentUser;

    private Dialog dialog;
    private EditText inputEditText;
    private ImageView selectedImageView;
    private Button registerButton;

    private Map<Marker, String> markerUserIdMap = new HashMap<>();
    private String markerId;

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
        currentUser = auth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        userId = auth.getUid();
        username = currentUser.getDisplayName();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        renewProfile();

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
                        if (dialog != null && dialog.isShowing()) { // 다이얼로그가 열려 있는 경우
                            updateImage(selectedImageView); // 이미지 갱신
                            updateRegisterButtonState(inputEditText, selectedImageView, registerButton); // 버튼 상태 갱신
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

    // 프로필 갱신
    private void renewProfile() {
        View headerView = binding.ecomapNavigationView.getHeaderView(0);
        CircleImageView profileImage = headerView.findViewById(R.id.navigationHeader_profileImage);
        TextView profileName = headerView.findViewById(R.id.navigationHeader_profileName);
        TextView profileUsername = headerView.findViewById(R.id.navigationHeader_profileUsername);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        userId = auth.getUid();

        firestore.collection("프로필").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ProfileModel profile = documentSnapshot.toObject(ProfileModel.class);

                        if (profile != null) {
                            profileName.setText(profile.getUsername());
                            profileUsername.setText(profile.getEmail());

                            if (profile.getPath() != null) {
                                FirebaseStorage.getInstance().getReference(profile.getPath())
                                        .getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            Glide.with(MainActivity.this) // 컨텍스트 명시적으로 전달
                                                    .load(uri)
                                                    .placeholder(R.drawable.profile_pic)
                                                    .error(R.drawable.profile_pic)
                                                    .into(profileImage);
                                        });
                            }
                        } else {
                            Toast.makeText(this, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "사용자 정보를 가져오는 중 오류 발생!: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Toolbar 및 Drawer 설정
    private void setupToolbarAndDrawer() {
        binding.ecomapTopAppBar.setNavigationOnClickListener(view -> binding.ecomapDrawerLayout.openDrawer(GravityCompat.START));
        binding.ecomapNavigationView.setVisibility(View.VISIBLE);

        renewProfile();

        binding.ecomapNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            binding.ecomapDrawerLayout.closeDrawer(GravityCompat.START);

            String message = "";
            Intent intent = null;

            if (id == R.id.navigationItems_photoShare) {
                showInputDialog();
            } else if (id == R.id.navigationItems_askQuestion) {

                intent = new Intent(MainActivity.this, CreateNewQuestionActivity.class);
                startActivity(intent);
            } else if (id == R.id.navigationItems_queryBoard) {

                intent = new Intent(MainActivity.this, QuestionListPreviewActivity.class);
                startActivity(intent);
            } else if (id == R.id.navigationItems_photoBoard) {
                message = "사진을 구경하세요";
            } else if (id == R.id.navigationItems_settings) {

                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);

                renewProfile();
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
        selectedImageUri = null;

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_input);

        inputEditText = dialog.findViewById(R.id.edit_marker_title);
        Button selectImageButton = dialog.findViewById(R.id.btn_select_image);
        registerButton = dialog.findViewById(R.id.btn_register_marker);
        ImageView closeButton = dialog.findViewById(R.id.btn_close_dialog);
        selectedImageView = dialog.findViewById(R.id.selected_image_view);

        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        // 초기 상태: 버튼 비활성화
        registerButton.setEnabled(false);

        // 제목 입력 필드 텍스트 변경 감지
        inputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateRegisterButtonState(inputEditText, selectedImageView, registerButton);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // 사진 선택 버튼 클릭 리스너
        selectImageButton.setOnClickListener(view -> openImagePicker());

        // 마커 등록 버튼 클릭 리스너
        registerButton.setOnClickListener(v -> {
            String markerTitle = inputEditText.getText().toString();
            addMarkerAtCurrentLocation(markerTitle, selectedImageUri);
            dialog.dismiss();
        });

        dialog.show();
        updateImage(selectedImageView);
        updateRegisterButtonState(inputEditText, selectedImageView, registerButton);
    }

    // 선택 이미지 미리보기
    private void updateImage(ImageView selectedImageView) {
        if (selectedImageUri != null) {
            selectedImageView.setVisibility(View.VISIBLE);
            selectedImageView.setImageURI(selectedImageUri);
        } else {
            selectedImageView.setVisibility(View.GONE);
        }
    }

    // 등록 버튼 화설화 / 비활성화
    private void updateRegisterButtonState(EditText inputEditText, ImageView selectedImageView, Button registerButton) {
        String titleText = inputEditText.getText().toString().trim();
        boolean hasTitle = !titleText.isEmpty(); // 제목이 입력되었는지 확인
        boolean hasImage = selectedImageUri != null; // 이미지 URI를 직접 확인

        // 제목과 이미지가 모두 있는 경우에만 버튼 활성화
        registerButton.setEnabled(hasTitle && hasImage);
    }

    // 이미지 Picker 호출
    private void openImagePicker() {
        Intent intent = new Intent(this, Picture.class);
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

            uploadMarker(markerTitle, imageUri, new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            Toast.makeText(this, "위치 찾을 수 없음", Toast.LENGTH_SHORT).show();
        }
    }

    // 마커 업로드
    private void uploadMarker(String name, Uri imageUri, @NonNull LatLng location) {
        long timestamp = System.currentTimeMillis();
        String storagePath = "markers/" + timestamp + ".jpg";

        StorageReference imageRef = FirebaseStorage.getInstance().getReference(storagePath);

        imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Map<String, Object> markerData = new HashMap<>();
                    markerData.put("name", name);
                    markerData.put("userId", userId);
                    markerData.put("timestamp", timestamp);
                    markerData.put("photoPath", downloadUri.toString());
                    markerData.put("latitude", location.latitude);
                    markerData.put("longitude", location.longitude);

                    firestore.collection("마커")
                            .add(markerData)
                            .addOnSuccessListener(docRef -> Log.d("markers", "마커 업로드 완료"))
                            .addOnFailureListener(e -> Log.e("markers", "마커 업로드 실패: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("markers", "이미지 업로드 실패: " + e.getMessage()));
    }

    // Firestore에서 마커 불러오기
    private void fetchMarkers(@NonNull GoogleMap googleMap) {
        VisibleRegion visibleRegion = googleMap.getProjection().getVisibleRegion();
        LatLngBounds bounds = visibleRegion.latLngBounds;

        firestore.collection("마커")
                .whereGreaterThanOrEqualTo("latitude", bounds.southwest.latitude)
                .whereLessThanOrEqualTo("latitude", bounds.northeast.latitude)
                .whereGreaterThanOrEqualTo("longitude", bounds.southwest.longitude)
                .whereLessThanOrEqualTo("longitude", bounds.northeast.longitude)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    googleMap.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        double latitude = document.getDouble("latitude");
                        double longitude = document.getDouble("longitude");
                        String name = document.getString("name");
                        String photoPath = document.getString("photoPath");
                        String markerUserId = document.getString("userId");

                        LatLng position = new LatLng(latitude, longitude);
                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(name));

                        if (marker != null) {
                            marker.setTag(photoPath);
                        }

                        markerUserIdMap.put(marker, markerUserId);
                    }
                })
                .addOnFailureListener(e -> Log.e("markerFetch", "마커 로드 실패: " + e.getMessage()));
    }

    // 마커 클릭 시 Dialog 표시
    private void showMarkerDialog(@NonNull Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String markerTitle = marker.getTitle();
        builder.setTitle(markerTitle);


        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_marker, null);

        ImageView markerImageView = dialogView.findViewById(R.id.maker_image);
        Button confirmButton = dialogView.findViewById(R.id.btn_confirm);

        TextView deleteButton = dialogView.findViewById(R.id.delete_marker);

        String markerUserId = markerUserIdMap.get(marker);
        updateDeleteMarker(dialogView, markerUserId);

        // 마커 태그 가져오기
        Object tag = marker.getTag();
        if (tag instanceof String) { // 태그가 String인지 확인
            String imageUrl = (String) tag;
            if (!imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .into(markerImageView);
                markerImageView.setVisibility(View.VISIBLE);
            } else {
                markerImageView.setVisibility(View.GONE);
                Log.d("markers", "이미지가 없음");
            }
        } else {
            markerImageView.setVisibility(View.GONE);
            Log.e("markers", "유효하지 않은 태그 형식");
        }

        AlertDialog dialog = builder.setView(dialogView).create();
        confirmButton.setOnClickListener(v -> dialog.dismiss());

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                deleteMarker(marker);
            }
        });

        dialog.show();
    }

    // 마커 삭제 버튼 (비)활성화
    private void updateDeleteMarker(View dialogView, String mId) {
        renewProfile();
        TextView delete = dialogView.findViewById(R.id.delete_marker);
        markerId = mId;
        if (userId.equals(this.markerId)) { // this.markerId로 클래스 필드 참조
            delete.setVisibility(View.VISIBLE);
        } else {
            delete.setVisibility(View.GONE);
        }
    }

    // 마커 삭제
    private void deleteMarker(Marker marker) {
        LatLng position = marker.getPosition();

        firestore.collection("마커")
                .whereEqualTo("latitude", position.latitude)
                .whereEqualTo("longitude", position.longitude)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String documentId = document.getId();

                            firestore.collection("마커")
                                    .document(documentId)
                                    .delete()
                                    .addOnSuccessListener(s -> {
                                        Toast.makeText(this, "삭제가 완료되었습니다", Toast.LENGTH_SHORT).show();
                                        marker.remove();
                                        fetchMarkers(myMap);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "삭제를 실패했습니다", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "마커를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "삭제할 없습니다", Toast.LENGTH_SHORT).show());

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
        fetchMarkers(googleMap);

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
                fetchMarkers(googleMap);
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