package com.otowave.otowave;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private TextView titleTv, artistTv, currentTimeTv, totalTimeTv;
    private SeekBar seekBar;
    private FloatingActionButton playPauseBtn;
    private AudioModel currentSong;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);

        titleTv = findViewById(R.id.player_song_title);
        artistTv = findViewById(R.id.player_artist_name);
        currentTimeTv = findViewById(R.id.player_current_time);
        totalTimeTv = findViewById(R.id.player_total_time);
        seekBar = findViewById(R.id.player_seekbar);
        playPauseBtn = findViewById(R.id.fab_play_pause);

        currentSong = (AudioModel) getIntent().getSerializableExtra("SONG");

        if (currentSong != null) {
            titleTv.setText(currentSong.getTitle());
            artistTv.setText(currentSong.getArtist());
            playMusic(currentSong.getPath());
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        playPauseBtn.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn_back).getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void playMusic(String path) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            
            seekBar.setMax(mediaPlayer.getDuration());
            totalTimeTv.setText(formatDuration(mediaPlayer.getDuration()));
            
            updateSeekBar();

        } catch (IOException e) {
            Toast.makeText(this, "Error playing music", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            currentTimeTv.setText(formatDuration(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatDuration(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}