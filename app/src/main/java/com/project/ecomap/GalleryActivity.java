package com.project.ecomap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.project.ecomap.databinding.ActivityGalleryBinding;

import java.util.ArrayList;
import java.util.List;

//마커 컬렉션에 liked 추가해야함, 기본셋0(혜준)
public class GalleryActivity extends AppCompatActivity{

    ActivityGalleryBinding binding;
    private ImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageAdapter = new ImageAdapter(this, new ArrayList<>());
        binding.recyclerView.setAdapter(imageAdapter);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerView.setHasFixedSize(true);

        //기본: 최신순 정렬
        sortImages("timestamp",true);
        binding.recentImage.setChecked(true);

        //라디오버튼 - 최신순, 인기순
        binding.recentImage.setOnClickListener(v->sortImages("timestamp", true));
        binding.likedImage.setOnClickListener(v->sortImages("likeCount", true));

        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    //이미지 정렬순 불러오기
    private void sortImages(String orderby, boolean isDescending) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("마커")
                .orderBy(orderby, isDescending ? Query.Direction.DESCENDING : Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("gallery_log", "마커 데이터 불러오기 실패");
                        return;
                    }

                    if (value != null) {
                        List<Image> imageList = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            String imageUrl = document.getString("imageUrl");
                            String title = document.getString("title");
                            Long likeCountObj = document.getLong("likeCount");
                            long likedCount = (likeCountObj != null) ? likeCountObj : 0;

                            if (imageUrl != null && title != null) {
                                Image image = new Image();
                                image.setMarkerId(document.getId());
                                image.setImageUrl(imageUrl);
                                image.setTitle(title);

                                image.setLikedCount(likedCount);
                                imageList.add(image);
                            }
                        }
                        Log.d("gallery_log", "마커 이미지 불러오기 성공");
                        imageAdapter.setImageList(imageList);
                    }
                });
    }

    void loadImage(String imageUrl){
        Glide.with(this)
                .load(imageUrl)
                .into(binding.fullImage);
        binding.fullImageFrame.setVisibility(View.VISIBLE);
    }

}