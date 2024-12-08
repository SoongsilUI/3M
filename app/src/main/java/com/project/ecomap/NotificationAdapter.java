package com.project.ecomap;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    Context context;
    List<Notification> notificationList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notification notification);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        Notification  notification = notificationList.get(position);
        holder.notificationTitle.setText(notification.getTitle());
        holder.notificationTime.setText(notification.getTime());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Questions")
                .document(notification.getQuestionId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String questionTitle = documentSnapshot.getString("title");
                        holder.notificationTitle.setText(questionTitle);
                    } else {
                        holder.notificationTitle.setText("삭제된 질문");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.notificationTitle.setText("질문 불러오기 실패");
                    Log.e("Jo", "notification에서 질문제목 불러오기 실패", e);
                });

        holder.itemView.setOnClickListener(v -> {

            if(listener!=null){
                listener.onItemClick(notification);
            }
            Intent intent = new Intent(context, QuestionPostActivity.class);
            db.collection("프로필")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("notifications")
                    .document(notification.getQuestionId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {

                        notificationList.remove(position);
                        notifyItemRemoved(position);
                        Log.d("Jo", "알림 삭제 성공");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Jo", "알림 삭제 실패", e);
                    });
            intent.putExtra("questionId", notification.getQuestionId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle;
        TextView notificationTime;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);
            notificationTime = itemView.findViewById(R.id.notificationTime);
        }
    }


}
