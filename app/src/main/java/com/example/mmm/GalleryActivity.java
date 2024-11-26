package com.example.mmm;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity{

    RecyclerView recyclerView;
    ArrayList<Image> imageList;
    ImageAdapter adapter;
    ImageView backButton;
    RadioButton recentImage, likedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        recyclerView = findViewById(R.id.recyclerView);
        backButton = findViewById(R.id.backButton);
        recentImage = findViewById(R.id.recentImage);
        likedImage = findViewById(R.id.likedImage);

        imageList = new ArrayList<>();
        adapter = new ImageAdapter(this, imageList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        recentImage.setOnClickListener(v->sortImages("timestamp", true));
        likedImage.setOnClickListener(v->sortImages("likes", false));

        sortImages("likes", true);
    }

    private void sortImages(String orderby, boolean isDescending) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        firestore.collection("Images")
                .orderBy(orderby, isDescending? Query.Direction.DESCENDING: Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    imageList.clear();
                    for(DocumentSnapshot dc : queryDocumentSnapshots) {
                        Image image = dc.toObject(Image.class);
                        
                        storage.getReferenceFromUrl(image.getImageURL())
                                .getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    image.setImageURL(uri.toString());
                                    imageList.add(image);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }
}