package com.harsh.shah.saavnmp3.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.model.LocalSong;
import com.harsh.shah.saavnmp3.utils.LocalMusicHelper;

import java.util.List;

public class LocalMusicFragment extends Fragment {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local, container, false);
        recyclerView = view.findViewById(R.id.local_songs_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadLocalSongs();
        return view;
    }

    private void loadLocalSongs() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            List<LocalSong> songs = LocalMusicHelper.getLocalSongs(requireContext());
            recyclerView.setAdapter(new com.harsh.shah.saavnmp3.fragments.LocalSongsAdapter(songs));
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadLocalSongs();
            }
        }
    }
}