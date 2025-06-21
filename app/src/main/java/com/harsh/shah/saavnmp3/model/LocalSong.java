package com.harsh.shah.saavnmp3.model;

public class LocalSong {
    private String title;
    private String artist;
    private String path;
    private long duration;

    // Constructor
    public LocalSong(String title, String artist, String path, long duration) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
    }

    // Getters (¡estos son esenciales para el adaptador!)
    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    public long getDuration() {
        return duration;
    }
}