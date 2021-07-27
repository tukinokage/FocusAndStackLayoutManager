package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * PACK com.example.myapplication
 * CREATE BY Shay
 * DATE BY 2021/7/8 13:46 星期四
 * <p>
 * DESCRIBE
 * <p>
 */
// TODO:2021/7/8 

public class MyRvAdapter extends RecyclerView.Adapter<MyRvAdapter.MyViewHolder> {
    private Context mContext;
    private List<Integer> picRidList = new ArrayList<>();

    public void setPicRidList(List<Integer> picRidList) {
        this.picRidList = picRidList;
    }

    public void addPic(int rid){
        picRidList.add(rid);
    }

    public MyRvAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.text_rv_item, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.imageView.setImageResource(picRidList.get(position));
    }

    @Override
    public int getItemCount() {
        return picRidList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView imageView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_iv);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("MyRvAdapter item test", "create");
                }
            });
        }
    }
}
