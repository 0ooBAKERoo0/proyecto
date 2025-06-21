package com.harsh.shah.saavnmp3.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.adapters.LocalMusicAdapter;
import com.harsh.shah.saavnmp3.model.LocalSong;
import com.harsh.shah.saavnmp3.services.PlayerService;
import com.harsh.shah.saavnmp3.utils.LocalMusicHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalMusicFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<LocalSong> localSongs = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Habilitar el menú en el fragmento
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadLocalSongs();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_nav, menu); // Inflar el menú
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.local) {
            // Acción cuando se selecciona el ítem del menú
            Toast.makeText(getContext(), "Local Music Selected", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLocalSongs() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            localSongs = LocalMusicHelper.getLocalSongs(requireContext());
            LocalMusicAdapter adapter = new LocalMusicAdapter(localSongs, this::playSong); // Pasamos el método playSong
            recyclerView.setAdapter(adapter);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 123 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadLocalSongs();
        } else {
            Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para reproducir la canción seleccionada
    private void playSong(LocalSong song) {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        intent.setAction(PlayerService.ACTION_PLAY);
        intent.putExtra("song_path", song.getPath());
        requireContext().startService(intent);
    }
}