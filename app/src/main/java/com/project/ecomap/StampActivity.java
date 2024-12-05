package com.project.ecomap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.ecomap.databinding.ActivityStampBinding;

import java.util.ArrayList;
import java.util.List;

public class StampActivity extends AppCompatActivity {
    String trail_name;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStampBinding binding = ActivityStampBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        RecyclerView recyclerView = binding.recyclerView;

        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", 0.0);
        double longitude = intent.getDoubleExtra("longitude", 0.0);

        // Adapter 초기화
        TrailAdapter adapter = new TrailAdapter(new ArrayList<>(), trail -> {
            Intent detailIntent = new Intent(StampActivity.this, StampMapActivity.class);
            detailIntent.putExtra("trail_name",trail_name);
            startActivity(detailIntent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        trail_name = getPlace(latitude,longitude);
        // Firebase에서 데이터 가져오기
        getTrailDataFromFirebase(adapter);
    }

    private void getTrailDataFromFirebase(TrailAdapter adapter) {
        db= FirebaseFirestore.getInstance();
        db.collection("trails")
                .document("seoul")
                .collection(trail_name)// Firestore에서 참조
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Trail> trails = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String description = document.getString("description");
                            trails.add(new Trail(name, description));
                        }
                        // Adapter에 새로운 데이터 설정
                        adapter.trailList.clear();
                        adapter.trailList.addAll(trails);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    private String getPlace(double latitude, double longitude) {
        if (latitude >= 37.4800 && latitude <= 37.5200 &&
                longitude >= 126.9000 && longitude <= 126.9800) {
            return "dongjak"; // 동작구
        }else{
            return "unknown_place";
        }
    }

    // Trail 데이터 클래스
    public static class Trail {
        private final String name;
        private final String description;

        public Trail(String name, String description) {
            this.name = name;
            this.description = description;
        }
        public String getName(){
            return name;
        }
        public String getDescription(){
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
