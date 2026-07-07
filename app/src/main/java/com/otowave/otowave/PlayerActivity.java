package com.otowave.otowave;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.otowave.otowave.databinding.ActivityPlayerBinding;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private MusicService musicService;
    private boolean isBound = false;
    private final Handler handler = new Handler();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            setupUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        binding.btnBack.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack.getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupUI() {
        if (musicService.getCurrentSong() == null) return;
        
        AudioModel song = musicService.getCurrentSong();
        binding.playerSongTitle.setText(song.getTitle());
        binding.playerArtistName.setText(song.getArtist());
        binding.playerTotalTime.setText(formatDuration(musicService.getDuration()));
        binding.playerSeekbar.setMax(musicService.getDuration());

        Glide.with(this)
                .load(getAlbumArtUri(song.getAlbumId()))
                .placeholder(R.drawable.ic_default_album_art)
                .into(binding.playerAlbumArt);

        binding.fabPlayPause.setOnClickListener(v -> {
            musicService.pauseResume();
            updatePlayPauseIcon();
        });

        binding.btnShuffle.setOnClickListener(v -> {
            musicService.setShuffle(!musicService.isShuffle());
            updateShuffleIcon();
        });

        binding.btnNext.setOnClickListener(v -> {
            musicService.nextSong();
            setupUI();
        });

        binding.btnPrevious.setOnClickListener(v -> {
            musicService.previousSong();
            setupUI();
        });

        binding.playerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) musicService.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updatePlayPauseIcon();
        updateShuffleIcon();
        updateSeekBar();
    }

    private void updateShuffleIcon() {
        if (musicService.isShuffle()) {
            binding.btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.primary));
        } else {
            binding.btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void updatePlayPauseIcon() {
        binding.fabPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateSeekBar() {
        if (isBound && musicService.isPlaying()) {
            int currentPos = musicService.getCurrentPosition();
            binding.playerSeekbar.setProgress(currentPos);
            binding.playerCurrentTime.setText(formatDuration(currentPos));
        }
        handler.postDelayed(this::updateSeekBar, 1000);
    }

    private Uri getAlbumArtUri(long albumId) {
        return Uri.parse("content://media/external/audio/albumart/" + albumId);
    }

    private String formatDuration(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);
    }
}