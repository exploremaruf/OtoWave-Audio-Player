package com.otowave.otowave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<AudioModel> songList = new ArrayList<>();
    private int currentSongIndex = 0;
    private boolean isShuffle = false;
    private final Random random = new Random();
    private static final String CHANNEL_ID = "OtoWave_Music_Channel";
    private static final int NOTIFICATION_ID = 1;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setList(List<AudioModel> songs) {
        songList = songs;
    }

    public void playSong(int index) {
        currentSongIndex = index;
        AudioModel song = songList.get(currentSongIndex);
        
        if (requestAudioFocus()) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(song.getPath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                startForeground(NOTIFICATION_ID, buildNotification());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pauseResume() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            if (requestAudioFocus()) {
                mediaPlayer.start();
            }
        }
        updateNotification();
    }

    public void nextSong() {
        if (songList.isEmpty()) return;
        
        if (isShuffle) {
            currentSongIndex = random.nextInt(songList.size());
        } else {
            currentSongIndex = (currentSongIndex + 1) % songList.size();
        }
        playSong(currentSongIndex);
    }

    public void previousSong() {
        if (songList.isEmpty()) return;

        if (isShuffle) {
            currentSongIndex = random.nextInt(songList.size());
        } else {
            currentSongIndex--;
            if (currentSongIndex < 0) currentSongIndex = songList.size() - 1;
        }
        playSong(currentSongIndex);
    }

    public void setShuffle(boolean shuffle) {
        isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    public AudioModel getCurrentSong() {
        if (songList.isEmpty()) return null;
        return songList.get(currentSongIndex);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        nextSong();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                break;
        }
        updateNotification();
    }

    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        AudioModel song = getCurrentSong();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle(song != null ? song.getTitle() : "OtoWave")
                .setContentText(song != null ? song.getArtist() : "Offline Music Player")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}