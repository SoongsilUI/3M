package com.example.mmm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;

public class ImageAdapter  extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{
    Context context;
    ArrayList<Image> dataList;
    OnItemClickListener onItemClickListener;

    public ImageAdapter(Context context, ArrayList<Image> dataList) {
        this.context = context;
        this.dataList=dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(context).load(dataList.get(position).getUrl()).into(holder.imageView);
        holder.itemView.setOnClickListener(view -> onItemClickListener.onClick(dataList.get(position)));

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.previewImage);
        }

    }

    public  interface  OnItemClickListener {
        void onClick (Image image);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
