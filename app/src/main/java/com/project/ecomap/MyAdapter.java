package com.project.ecomap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;


public class MyAdapter<T> extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    Context context;
    ArrayList<T> dataList;

    public MyAdapter(Context context, ArrayList<T> dataList) {
        this.context = context;
        this.dataList  = dataList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;

        // 데이터 타입에 따라 layout 선택
        if(dataList.get(0) instanceof Question) {
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
        if(item instanceof Question) {
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

        // Question Type 데이터 처리
        if(item instanceof Question) {
            Question question = (Question) item;

            if (question.getQTimestamp() != null && holder.qTimestamp != null) {
                //Date date = question.getQTimestamp().toDate();
               /* SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                String timeStampString = dateFormat.format(date);*/
                //String timeStampString = question.getTimeStampString();

                // timestamp를 포맷해서 뷰에 설정
                holder.qTimestamp.setText(question.getTimeStampString());
            }
            // 제목, 내용 설정
            if (holder.title != null){
                holder.title.setText(question.getTitle());
            }
            if (holder.content != null) {
                holder.content.setText(question.getContent());
            }

            // 아이템 클릭 이벤트
            holder.itemView.setOnClickListener(view -> {
                DocumentReference questionRef = db.collection("Questions").document(question.getQuestionId());
                if(question.getQuestionId() != null) {
                    Intent intent = new Intent(context, QuestionPostActivity.class);
                    intent.putExtra("questionId", question.getQuestionId()); // question id 전달
                    context.startActivity(intent);
                } else {
                    Log.e("MyAdapter", "questionId is null");
                }
            });
        }

        // Comment Type 데이터 처리
        else if (item instanceof Comment) {
            Comment comment = (Comment) item;

            // timestamp를 포맷해서 뷰에 설정
            if (comment.getCTimestamp() != null && holder.cTimestamp != null) {
                Date date = comment.getCTimestamp().toDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                String timeStampString = dateFormat.format(date);
                holder.cTimestamp.setText(timeStampString);
            }

            // 댓글 내용, 작성자 설정
            if(holder.comment != null) {
                holder.comment.setText(comment.getComment());
            }
            if(holder.commenter != null) {
                holder.commenter.setText(comment.getCommenter());
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView title, content;
        TextView comment, commenter, qTimestamp, cTimestamp;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            content = itemView.findViewById(R.id.content);
            commenter = itemView.findViewById(R.id.commenter);
            comment = itemView.findViewById(R.id.comment);
            qTimestamp = itemView.findViewById(R.id.qTimestamp);
            cTimestamp = itemView.findViewById(R.id.cTimestamp);
        }
    }

    // 아이템 이동 처리
    public void onItemMoved(int  fromPosition, int toPosition) {
        Collections.swap(dataList, fromPosition, toPosition); // 데이터 리스트 위치 변경
        notifyItemMoved(fromPosition, toPosition); // RecyclerView에 변경 알림
    }
}
