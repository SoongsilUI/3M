package com.project.ecomap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;


public class MyAdapter<T> extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    Context context;
    ArrayList<T> dataList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Question question);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public MyAdapter(Context context, ArrayList<T> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;

        // 데이터 타입에 따라 layout 선택
        if (dataList.get(0) instanceof Question) {
            v = LayoutInflater.from(context).inflate(R.layout.item_q, parent, false);
        } else {
            v = LayoutInflater.from(context).inflate(R.layout.item_c, parent, false);
        }
        return new MyViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        T item = dataList.get(position);

        // 데이터 타입에 따라 ViewType 반환
        if (item instanceof Question) {
            return 0; // Question Type
        } else if (item instanceof Comment) {
            return 1; // Comment Type
        }
        return -1; // unknown
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        T item = dataList.get(position); // 현재 아이템 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StorageReference storage = FirebaseStorage.getInstance().getReference();

        // Question Type 데이터 처리
        if (item instanceof Question) {
            Question question = (Question) item;

            if (question.getQTimestamp() != null && holder.qTimestamp != null) {
                // timestamp를 포맷해서 뷰에 설정
                holder.qTimestamp.setText(question.getTimeStampString());
            }
            // 제목, 내용 설정
            if (holder.title != null) {
                holder.title.setText(question.getTitle());
            }
            if (holder.content != null) {
                holder.content.setText(question.getContent());
            }
            String imageUrl = question.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                holder.previewImage.setVisibility(View.VISIBLE);
                holder.content.setEms(17);
                holder.content.setMaxLines(3);
                loadWithGlide(holder.previewImage, imageUrl);

            } else {
                holder.previewImage.setVisibility(View.GONE);
                holder.content.setEms(20);
                holder.content.setMaxLines(1);
            }

            // 아이템 클릭 이벤트
            holder.itemView.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onItemClick(question);
                }
            });
        }
        // Comment Type 데이터 처리
        else if (item instanceof Comment) {
            Comment comment = (Comment) item;
            FirebaseAuth auth= FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            String userId = currentUser.getUid();

            // timestamp를 포맷해서 뷰에 설정
            if (comment.getCTimestamp() != null && holder.cTimestamp != null) {

                Date date = comment.getCTimestamp().toDate();

                long currentTime = System.currentTimeMillis();
                long commentTime = date.getTime();
                long passedTime = currentTime - commentTime;
                long passedDay = passedTime / (1000 * 60 * 60 * 24);
                String timeStampString;
                if (passedDay >= 1) {
                    SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                    timeStampString = dateFormat2.format(date);

                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                    timeStampString = dateFormat.format(date);
                    holder.cTimestamp.setText(timeStampString);
                }
                holder.cTimestamp.setText(timeStampString);

            }

            // 댓글 내용, 작성자 설정
            if (holder.comment != null) {
                holder.comment.setText(comment.getComment());
            }
            if (holder.commenterName != null) {
                db.collection("프로필")
                        .document(comment.getCommenterId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String nickname = documentSnapshot.getString("username");
                            holder.commenterName.setText(nickname);
                        });
            }

            if (holder.cProfileImage != null) {
                db.collection("프로필")
                        .document(comment.getCommenterId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String photoPath = documentSnapshot.getString("path");
                            if (photoPath != null) {
                                storage.child(photoPath).getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            loadWithGlide(holder.cProfileImage, String.valueOf(uri));
                                        });

                            }
                        });

            }
            if(comment.getCommenterId().equals(userId)) {
                holder.deleteComment.setVisibility(View.VISIBLE);
                holder.deleteComment.setOnClickListener(view -> {
                    new AlertDialog.Builder(context)
                            .setMessage("정말 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> deleteComment(comment.getQuestionId(), comment.getCommentId()))
                            .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                            .show();
                });
            } else {
                holder.deleteComment.setVisibility(View.GONE);
            }
        }
    }
    private void deleteComment(String questionId, String commentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Questions").document(questionId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "댓글 삭제 실패", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView title, content;
        TextView comment, commenterName, qTimestamp, cTimestamp, deleteComment;
        ImageView previewImage, cProfileImage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            content = itemView.findViewById(R.id.content);
            commenterName = itemView.findViewById(R.id.commenter);
            comment = itemView.findViewById(R.id.comment);
            qTimestamp = itemView.findViewById(R.id.qTimestamp);
            cTimestamp = itemView.findViewById(R.id.cTimestamp);
            previewImage = itemView.findViewById(R.id.previewImage);
            cProfileImage = itemView.findViewById(R.id.profile);
            deleteComment = itemView.findViewById(R.id.deleteComment);

        }

    }

    //이미지 불러오기
    public void loadWithGlide(ImageView imageView, String imageUrl) {

        Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.image)
                .into(imageView);
    }


}
