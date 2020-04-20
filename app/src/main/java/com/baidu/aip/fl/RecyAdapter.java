/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.fl;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import uni.UNI6BD21A1.R;

public class RecyAdapter extends RecyclerView.Adapter<RecyAdapter.ViewHolder> implements View.OnClickListener {

    private Context context;
    private List<Bitmap> datas;

    public RecyAdapter(Context context) {
        this.context = context;
    }

    public void setDatas(List<Bitmap> datas) {
        this.datas = datas;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizon, parent, false);
        ViewHolder vh = new ViewHolder(view);
        view.setOnClickListener(this);
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (datas.size() <= 0) {
            return;
        }
        int newPos = position % datas.size();
        holder.img.setImageBitmap(datas.get(newPos));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        // return Integer.MAX_VALUE;
        int count = 0;
        if (datas != null) {
            count = datas.size();
        }
        return count;
    }

    @Override
    public void onClick(View view) {

    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;

        public ViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.img);
        }
    }
}