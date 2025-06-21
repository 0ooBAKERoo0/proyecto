package com.harsh.shah.saavnmp3.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.harsh.shah.saavnmp3.ApplicationClass;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.model.Song;

public class MusicService extends Service {

    // Constantes
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "MUSIC_CHANNEL";
    public static final String ACTION_PREV = "com.harsh.shah.saavnmp3.PREV";
    public static final String ACTION_PLAY_PAUSE = "com.harsh.shah.saavnmp3.PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.harsh.shah.saavnmp3.NEXT";
    public static final String ACTION_CLOSE = "com.harsh.shah.saavnmp3.CLOSE";

    // Variables de instancia
    private final IBinder mBinder = new MyBinder();
    private MediaSessionCompat mediaSession;
    private ActionPlaying actionPlaying;
    private NotificationManager notificationManager;

    // Estado de reproducción
    private Song currentSong;
    private boolean isPlaying = false;

    // Binder para la conexión con actividades
    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getExtras() == null) return START_STICKY;

        String actionName = intent.getAction() != null ? intent.getAction() : intent.getExtras().getString("action", "");
        if (actionName != null) {
            switch (actionName) {
                case ApplicationClass.ACTION_NEXT:
                case ACTION_NEXT:
                    if (actionPlaying != null) {
                        actionPlaying.nextClicked();
                    }
                    break;
                case ApplicationClass.ACTION_PREV:
                case ACTION_PREV:
                    if (actionPlaying != null) {
                        actionPlaying.prevClicked();
                    }
                    break;
                case ApplicationClass.ACTION_PLAY:
                case ACTION_PLAY_PAUSE:
                    if (actionPlaying != null) {
                        actionPlaying.playClicked();
                        isPlaying = !isPlaying;
                        updateNotification();
                    }
                    break;
                case "action_click":
                    String id = intent.getStringExtra("id");
                    if (id != null && !id.isEmpty()) {
                        Intent musicIntent = new Intent(this, MusicOverviewActivity.class);
                        musicIntent.putExtra("id", id);
                        musicIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(musicIntent);
                    }
                    break;
                case ACTION_CLOSE:
                    stopSelf();
                    break;
            }
        }

        return START_STICKY;
    }

    // Métodos públicos para controlar el servicio
    public void setCallback(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }

    public void showNotification(Song song, boolean isPlaying) {
        this.currentSong = song;
        this.isPlaying = isPlaying;

        // Mostrar notificación personalizada o estándar según la versión
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showCustomNotification(song);
        } else {
            showStandardNotification(song.getTitle(), song.getArtist(), isPlaying);
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void showCustomNotification(Song song) {
        // Crear RemoteViews para el layout personalizado
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.custom_notification);

        // Configurar textos
        notificationLayout.setTextViewText(R.id.song_title, song.getTitle());
        notificationLayout.setTextViewText(R.id.song_artist, song.getArtist());

        // Configurar icono de play/pause
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        notificationLayout.setImageViewResource(R.id.btn_play_pause, playPauseIcon);

        // Configurar PendingIntents para los botones
        notificationLayout.setOnClickPendingIntent(R.id.btn_prev,
                PendingIntent.getBroadcast(this, 0,
                        new Intent(ACTION_PREV),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        notificationLayout.setOnClickPendingIntent(R.id.btn_play_pause,
                PendingIntent.getBroadcast(this, 0,
                        new Intent(ACTION_PLAY_PAUSE),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        notificationLayout.setOnClickPendingIntent(R.id.btn_next,
                PendingIntent.getBroadcast(this, 0,
                        new Intent(ACTION_NEXT),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // Intent para cuando se hace clic en la notificación
        Intent clickIntent = new Intent(this, MusicOverviewActivity.class)
                .putExtra("action", "action_click")
                .putExtra("id", song.getId());
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Construir notificación
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setCustomContentView(notificationLayout)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setContentIntent(clickPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @SuppressLint("ForegroundServiceType")
    private void showStandardNotification(String title, String artist, boolean isPlaying) {
        // Crear intents para las acciones
        Intent prevIntent = new Intent(this, MusicService.class)
                .setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, MusicService.class)
                .setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class)
                .setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent para cuando se hace clic en la notificación
        Intent clickIntent = new Intent(this, MusicOverviewActivity.class)
                .putExtra("action", "action_click");
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Construir la notificación
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(getAlbumArt(currentSong))
                .addAction(
                        R.drawable.ic_skip_previous,
                        getString(R.string.notification_previous), // Usa el nuevo nombre
                        prevPendingIntent
                )
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying ? getString(R.string.pause) : getString(R.string.play),
                        playPendingIntent)
                .addAction(R.drawable.ic_skip_next, getString(R.string.next), nextPendingIntent)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setContentIntent(clickPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    // Métodos privados de ayuda
    private void updateNotification() {
        if (currentSong != null) {
            showNotification(currentSong, isPlaying);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.music_channel_name), // Nombre actualizado
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.music_channel_description));
            channel.setSound(null, null);
            channel.setShowBadge(false);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Bitmap getAlbumArt(Song song) {
        // Implementación básica - deberías reemplazarla con tu lógica real
        try {
            return BitmapFactory.decodeResource(getResources(), R.drawable.default_album);
        } catch (Exception e) {
            return BitmapFactory.decodeResource(getResources(), R.drawable.default_album);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
        stopForeground(true);
    }

    // Métodos para controlar el estado desde la actividad
    public void setCurrentSong(Song song) {
        this.currentSong = song;
        updateNotification();
    }

    public void setPlayingState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        updateNotification();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Song getCurrentSong() {
        return currentSong;
    }
}