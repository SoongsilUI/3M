package com.project.ecomap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.ecomap.databinding.ActivityStampBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StampActivity extends AppCompatActivity {
    String trail_name; // 스피너에서 선택된 trail_name 저장
    FirebaseFirestore db;
    Map<String, String> districtMap = new HashMap<>();
    private static final String TAG = "StampActivity_log";
    String selectedDistrict;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStampBinding binding = ActivityStampBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "StampActivity 시작됨");

        // 뒤로 가기 버튼 클릭 이벤트 처리
        binding.backButton.setOnClickListener(v -> {
            Log.d(TAG, "뒤로가기 버튼 클릭됨");
            finish();
        });

        // RecyclerView 초기화
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        TrailAdapter adapter = new TrailAdapter(new ArrayList<>(), trail -> {
            Log.d(TAG,  trail.getName() + " 선택됨");
            Intent detailIntent = new Intent(StampActivity.this, StampMapActivity.class);
            detailIntent.putExtra("ROUTE_CODE", districtMap.get(selectedDistrict)); // dongjak 을 넘겨줌
            startActivity(detailIntent);
            Log.d(TAG, "StampMapActivity로 이동");
        });
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView 초기화 완료");

        // 스피너 초기화
        Spinner spinner = binding.spinnerTrailName;
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.trail_names, // strings.xml에 정의된 배열
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        Log.d(TAG, "Spinner 초기화 완료");

        // 스피너 선택 이벤트 처리
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "선택된 지역 = " + selectedDistrict);
                trail_name = districtMap.get(selectedDistrict);
                Log.d(TAG, "선택된 trail_name = " + trail_name);
                getTrailDataFromFirebase(adapter); // Firestore에서 데이터 가져오기
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "아무것도 선택되지 않음");
            }
        });

        // Firebase Firestore 초기화
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firebase Firestore 초기화 완료");

        // 지역 이름 매핑
        districtMap.put("동작구", "dongjak");
        districtMap.put("종로구", "jongro");
        Log.d(TAG, "districtMap 초기화 완료");
    }

    private void getTrailDataFromFirebase(TrailAdapter adapter) {
        if (trail_name == null || trail_name.isEmpty()) {
            Log.d(TAG, "trail_name이 비어 있음");
            return;
        }
        Log.d(TAG, "trail_name = " + trail_name);

        db.collection("trails").document(trail_name).collection("trails")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Trail> trails = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String description = document.getString("description");
                            trails.add(new Trail(name, description));
                            Log.d(TAG, "Trail 추가됨 - 이름: " + name);
                        }
                        // RecyclerView 데이터 갱신
                        adapter.trailList.clear();
                        adapter.trailList.addAll(trails);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "RecyclerView 데이터 갱신 완료");
                    } else {
                        Log.e(TAG, "데이터 가져오기 실패", task.getException());
                    }
                });
    }

    // Trail 데이터 클래스
    public static class Trail {
        private final String name;
        private final String description;

        public Trail(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    // TrailAdapter 클래스
    public static class TrailAdapter extends RecyclerView.Adapter<TrailAdapter.TrailViewHolder> {
        private final List<Trail> trailList;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Trail trail);
        }

        public TrailAdapter(List<Trail> trailList, OnItemClickListener listener) {
            this.trailList = trailList;
            this.listener = listener;
        }

        // ViewHolder 클래스
        public static class TrailViewHolder extends RecyclerView.ViewHolder {
            private final TextView trailName;
            private final TextView trailDescription;

            public TrailViewHolder(View itemView) {
                super(itemView);
                trailName = itemView.findViewById(R.id.tv_route_name);
                trailDescription = itemView.findViewById(R.id.tv_route_description);
            }

            public void bind(Trail trail, OnItemClickListener listener) {
                trailName.setText(trail.getName());
                trailDescription.setText(trail.getDescription());
                itemView.setOnClickListener(v -> listener.onItemClick(trail));
            }
        }

        @Override
        @NonNull
        public TrailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trail, parent, false);
            return new TrailViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TrailViewHolder holder, int position) {
            holder.bind(trailList.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return trailList.size();
        }
    }
}
