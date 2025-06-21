package com.harsh.shah.saavnmp3.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import com.harsh.shah.saavnmp3.model.LocalSong;

import java.util.ArrayList;
import java.util.List;

public class LocalMusicHelper {
    public static List<LocalSong> getLocalSongs(Context context) {
        List<LocalSong> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Consulta a MediaStore...
        return songs;
    }
}
