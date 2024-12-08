package com.project.ecomap;

import android.content.Context;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<Image> imageList;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storage = FirebaseStorage.getInstance().getReference();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser = auth.getCurrentUser();
    private String userId = currentUser.getUid();

    public ImageAdapter(Context context, List<Image> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = imageList.get(position);
        holder.imageTitle.setText(image.getTitle());

        if(image.getImageUrl() !=null) {
            Glide.with(context)
                    .load(image.getImageUrl())
                    .into(holder.imagePreview);
        }
        db.collection("마커").document(image.getMarkerId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long likeCountObj = documentSnapshot.getLong("likeCount");
                        long likeCount =  (likeCountObj != null) ? likeCountObj : 0;
                        holder.likedCount.setText(String.valueOf(likeCount));

                        db.collection("좋아요")
                                .document(userId)
                                .collection("마커")
                                .document(image.getMarkerId())
                                .get()
                                .addOnSuccessListener(likeDoc -> {
                                    boolean isLiked = likeDoc.exists();
                                    holder.likeButton.setImageResource(isLiked ? R.drawable.favorite : R.drawable.favorite_border);
                                });
                    } else {
                        holder.likedCount.setText("0");
                    }
                });

        holder. likeButton.setOnClickListener(v -> {
            db.collection("좋아요")
                    .document(userId)
                    .collection("마커")
                    .document(image.getMarkerId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            db.collection("좋아요")
                                    .document(userId)
                                    .collection("마커")
                                    .document(image.getMarkerId())
                                    .delete();

                            db.collection("마커").document(userId)
                                    .update("likeCount", FieldValue.increment(-1));

                            holder.likeButton.setImageResource(R.drawable.favorite_border);
                            Long likeCountObj = documentSnapshot.getLong("likeCount");
                            long likeCount =  (likeCountObj != null) ? likeCountObj : 0;

                            holder.likedCount.setText(String.valueOf(likeCount));
                        } else {
                            Map<String, Object> likeData = new HashMap<>();
                            likeData.put("markerId", userId);
                            likeData.put("timestamp", System.currentTimeMillis());

                            db.collection("좋아요")
                                    .document(userId)
                                    .collection("마커")
                                    .document(image.getMarkerId())
                                    .set(likeData);

                            db.collection("마커").document(image.getMarkerId())
                                    .update("likeCount", FieldValue.increment(1));

                            holder.likeButton.setImageResource(R.drawable.heart_filled);
                            Long likeCountObj = documentSnapshot.getLong("likeCount");
                            long likeCount = (likeCountObj != null) ? likeCountObj : 0;
                            holder.likedCount.setText(String.valueOf(likeCount));
                        }
                    });
        });

        holder.imagePreview.setOnClickListener(v -> {
            GalleryActivity activity = (GalleryActivity) context;
            activity.loadImage(image.getImageUrl());

        });

    }



    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public void setImageList(List<Image> newImageList) {
        this.imageList = newImageList;
        notifyDataSetChanged();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview,closeButton, fullImage;;
        TextView imageTitle, likedCount;
        ImageButton likeButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            imageTitle = itemView.findViewById(R.id.image_title);
            likedCount = itemView.findViewById(R.id.liked_count);
            likeButton = itemView.findViewById(R.id.like_button);
        }
    }
}

