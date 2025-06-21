package com.harsh.shah.saavnmp3.model;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String imageUrl;
    // Añade otros campos según necesites

    public Song(String id, String title, String artist, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
