package com.harsh.shah.saavnmp3.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.harsh.shah.saavnmp3.model.LocalSong;

import java.util.ArrayList;
import java.util.List;

public class LocalMusicHelper {
    public static List<LocalSong> getLocalSongs(Context context) {
        List<LocalSong> songs = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA, // Ruta
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(0);
                    String artist = cursor.getString(1);
                    String path = cursor.getString(2);
                    long duration = cursor.getLong(3);

                    // Usa el constructor con parámetros
                    LocalSong song = new LocalSong(title, artist, path, duration);
                    songs.add(song);
                }
            }
        }
        return songs;
    }
}