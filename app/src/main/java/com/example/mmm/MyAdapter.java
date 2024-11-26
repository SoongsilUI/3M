package com.example.mmm;

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
        if(item instanceof Question) {
            return 0;
        } else if (item instanceof Comment) {
            return 1;
        }
        return -1;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        T item = dataList.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if(item instanceof Question) {
            Question question = (Question) item;

            if (question.getQTimestamp() != null && holder.qTimestamp != null) {
                //Date date = question.getQTimestamp().toDate();
               /* SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                String timeStampString = dateFormat.format(date);*/
                //String timeStampString = question.getTimeStampString();
                holder.qTimestamp.setText(question.getTimeStampString());
            }
            if (holder.title != null){
                holder.title.setText(question.getTitle());
            }
            if (holder.content != null) {
                holder.content.setText(question.getContent());
            }

            holder.itemView.setOnClickListener(view -> {
                DocumentReference questionRef = db.collection("Questions").document(question.getQuestionId());
                if(question.getQuestionId() != null) {
                    Intent intent = new Intent(context, QuestionPostActivity.class);
                    intent.putExtra("questionId", question.getQuestionId());
                    context.startActivity(intent);
                } else {
                    Log.e("MyAdapter", "questionId is null");
                }
            });
        } else if (item instanceof Comment) {
            Comment comment = (Comment) item;

            if (comment.getCTimestamp() != null && holder.cTimestamp != null) {
                Date date = comment.getCTimestamp().toDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                String timeStampString = dateFormat.format(date);
                holder.cTimestamp.setText(timeStampString);
            }

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
/*onItemMoves - GPT*/
    public void onItemMoved(int  fromPosition, int toPosition) {
        Collections.swap(dataList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }



}
