package com.harsh.shah.saavnmp3.fragments;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.shah.saavnmp3.model.LocalSong;

import java.util.List;

public class LocalSongsAdapter extends RecyclerView.Adapter {
    public LocalSongsAdapter(List<LocalSong> songs) {
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
