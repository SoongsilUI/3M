package com.project.ecomap;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.project.ecomap.Models.MarkerModel;
public class MarkerAdapter extends FirestoreRecyclerAdapter<MarkerModel, MarkerAdapter.MarkerViewHolder> {
    public MarkerAdapter(FirestoreRecyclerOptions<MarkerModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MarkerAdapter.MarkerViewHolder holder, int position, @NonNull MarkerModel model) {
        holder.markerTitle.setText(model.getTitle());

        String imageUrl = model.getImageUrl();
        Log.d("MarkerAdapter", "Image URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .error(R.drawable.profile_pic) // 실패 시 대체 이미지
                    .into(holder.markerImage);
        } else {
            // 이미지 URL이 null이거나 비어 있을 경우 기본 이미지 설정
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.profile_pic)
                    .into(holder.markerImage);
        }

        holder.deleteButton.setOnClickListener(v -> {
            getSnapshots().getSnapshot(position).getReference().delete()
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(holder.itemView.getContext(), "삭제 완료", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(holder.itemView.getContext(), "삭제 실패", Toast.LENGTH_SHORT).show());
        });
    }


    @NonNull
    @Override
    public MarkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_marker, parent, false);
        return new MarkerViewHolder(view);
    }

    static class MarkerViewHolder extends RecyclerView.ViewHolder {
        ImageView markerImage;
        TextView markerTitle;
        Button deleteButton;

        public MarkerViewHolder(@NonNull View itemView) {
            super(itemView);
            markerImage = itemView.findViewById(R.id.markerImage);
            markerTitle = itemView.findViewById(R.id.markerTitle);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
