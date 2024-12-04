package com.project.ecomap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.ecomap.databinding.ActivitySearchBinding;

import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity implements OnMapReadyCallback {
    ActivitySearchBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private GoogleMap googleMap;
    private String searchQuery;

    private Map<Marker, String> markerUserIdMap;
    private Map<Marker, String> markerIdMap;

    private String userId;
    private String markerId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        userId = auth.getUid();
        firestore = FirebaseFirestore.getInstance();

        markerUserIdMap = new HashMap<>();
        markerIdMap = new HashMap<>();

        searchQuery = getIntent().getStringExtra("search_query");
        double currentLat = getIntent().getDoubleExtra("current_lat", 0);
        double currentLng = getIntent().getDoubleExtra("current_lng", 0);

        binding.searchQuery.setText(searchQuery+"에 대한 검색결과");

        binding.searchTopAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                this.googleMap = googleMap;

                // 현재 위치로 지도 이동
                if (currentLat != 0 && currentLng != 0) {
                    LatLng currentLocation = new LatLng(currentLat, currentLng);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); // Zoom level: 15
                }

                showMarkersForQuery(searchQuery);

                googleMap.setOnCameraIdleListener(() -> showMarkersForQuery(searchQuery));

                googleMap.setOnMarkerClickListener(marker -> {
                    showMarkerDialog(marker);
                    return true;
                });
            });
        }

    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        showMarkersForQuery(searchQuery);

        googleMap.setOnCameraIdleListener(() -> {
            showMarkersForQuery(searchQuery);
        });

        googleMap.setOnMarkerClickListener(marker -> {
            showMarkerDialog(marker);
            return true;
        });
    }

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
        String mId = markerIdMap.get(marker);
        updateDeleteMarker(dialogView, markerUserId);

        ImageView likeButton = dialogView.findViewById(R.id.like_button);
        TextView likeCountText = dialogView.findViewById(R.id.like_count);

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

        firestore.collection("마커").document(mId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long likeCount = documentSnapshot.getLong("likes");
                        likeCountText.setText(String.valueOf(likeCount));

                        firestore.collection("좋아요")
                                .document(userId)
                                .collection("마커")
                                .document(mId)
                                .get()
                                .addOnSuccessListener(likeDoc -> {
                                    boolean isLiked = likeDoc.exists();
                                    likeButton.setImageResource(isLiked ? R.drawable.heart_filled : R.drawable.heart_empty);
                                });
                    } else {
                        likeCountText.setText("0");
                    }
                });

        likeButton.setOnClickListener(v -> {
            firestore.collection("좋아요")
                    .document(userId)
                    .collection("마커")
                    .document(mId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            firestore.collection("좋아요")
                                    .document(userId)
                                    .collection("마커")
                                    .document(mId)
                                    .delete();

                            firestore.collection("마커").document(mId)
                                    .update("likes", FieldValue.increment(-1));

                            likeButton.setImageResource(R.drawable.heart_empty);
                        } else {
                            Map<String, Object> likeData = new HashMap<>();
                            likeData.put("markerId", mId);
                            likeData.put("timestamp", System.currentTimeMillis());

                            firestore.collection("좋아요")
                                    .document(userId)
                                    .collection("마커")
                                    .document(mId)
                                    .set(likeData);

                            firestore.collection("마커").document(mId)
                                    .update("likes", FieldValue.increment(1));

                            likeButton.setImageResource(R.drawable.heart_filled);
                        }
                    });
        });


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
                                        showMarkersForQuery(searchQuery);
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

    private void showMarkersForQuery(String query) {
        if (googleMap == null || query == null || query.isEmpty()) return;

        // 현재 화면의 지도 범위 가져오기
        VisibleRegion visibleRegion = googleMap.getProjection().getVisibleRegion();
        LatLngBounds bounds = visibleRegion.latLngBounds;

        firestore.collection("마커")
                .whereGreaterThanOrEqualTo("latitude", bounds.southwest.latitude)
                .whereLessThanOrEqualTo("latitude", bounds.northeast.latitude)
                .whereGreaterThanOrEqualTo("longitude", bounds.southwest.longitude)
                .whereLessThanOrEqualTo("longitude", bounds.northeast.longitude)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    googleMap.clear(); // 지도 초기화
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                            // 검색어가 이름에 포함된 경우만 추가
                            double latitude = document.getDouble("latitude");
                            double longitude = document.getDouble("longitude");
                            String photoPath = document.getString("photoPath");
                            String markerUserId = document.getString("userId");
                            String mId = document.getId();

                            LatLng position = new LatLng(latitude, longitude);
                            Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name));

                            if (marker != null) {
                                marker.setTag(photoPath);
                            }

                            markerUserIdMap.put(marker, markerUserId);
                            markerIdMap.put(marker, mId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("markerFetch", "마커 로드 실패: " + e.getMessage()));
    }


}