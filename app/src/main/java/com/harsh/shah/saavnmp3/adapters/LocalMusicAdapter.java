package com.harsh.shah.saavnmp3.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.model.LocalSong;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LocalMusicAdapter extends RecyclerView.Adapter<LocalMusicAdapter.ViewHolder> {
    private final List<LocalSong> songs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LocalSong song);
    }

    public LocalMusicAdapter(List<LocalSong> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LocalSong song = songs.get(position);
        holder.songTitle.setText(song.getTitle());
        holder.songArtist.setText(song.getArtist());
        String duration = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(song.getDuration()),
                TimeUnit.MILLISECONDS.toSeconds(song.getDuration()) % 60);
        holder.songDuration.setText(duration);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle, songArtist, songDuration;

        ViewHolder(View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
            songDuration = itemView.findViewById(R.id.song_duration);
        }
    }
}