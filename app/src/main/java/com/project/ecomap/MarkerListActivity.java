package com.project.ecomap;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.project.ecomap.Models.MarkerModel;
import com.project.ecomap.databinding.ActivityMarkerListBinding;

public class MarkerListActivity extends AppCompatActivity {
    ActivityMarkerListBinding binding;

    private FirebaseFirestore firestore;
    private MarkerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMarkerListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();

        String userId = getIntent().getStringExtra("userId");

        Query query = firestore.collection("마커")
                .whereEqualTo("userId", userId);

        FirestoreRecyclerOptions<MarkerModel> options = new FirestoreRecyclerOptions.Builder<MarkerModel>()
                .setQuery(query, MarkerModel.class)
                .build();

        adapter = new MarkerAdapter(options);
        RecyclerView recyclerView = findViewById(R.id.markerRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        binding.markerListTopAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
