package com.otowave.otowave;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.otowave.otowave.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private final List<AudioModel> audioList = new ArrayList<>();
    private ActivityMainBinding binding;
    private MusicService musicService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setList(audioList);
            updateMiniPlayer();
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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        binding.miniPlayer.setOnClickListener(v -> {
            if (musicService != null && musicService.getCurrentSong() != null) {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
            }
        });

        binding.btnPlayPause.setOnClickListener(v -> {
            if (isBound && musicService.getCurrentSong() != null) {
                musicService.pauseResume();
                updateMiniPlayer();
            }
        });

        binding.fabShuffle.setOnClickListener(v -> {
            if (isBound && !audioList.isEmpty()) {
                musicService.setShuffle(true);
                musicService.nextSong(); // This will pick a random song since shuffle is true
                updateMiniPlayer();
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkPermission();
        
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void checkPermission() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            loadAudioFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadAudioFiles();
            } else {
                Toast.makeText(this, "Permissions required to function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadAudioFiles() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    audioList.add(new AudioModel(
                            cursor.getString(titleCol),
                            cursor.getString(artistCol),
                            cursor.getString(dataCol),
                            cursor.getLong(durationCol),
                            cursor.getLong(albumIdCol)
                    ));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        MusicAdapter adapter = new MusicAdapter(audioList, this, (song, position) -> {
            if (isBound) {
                musicService.playSong(position);
                updateMiniPlayer();
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
            }
        });
        binding.recyclerView.setAdapter(adapter);
        if (isBound) musicService.setList(audioList);
    }

    private void updateMiniPlayer() {
        if (isBound && musicService.getCurrentSong() != null) {
            AudioModel song = musicService.getCurrentSong();
            binding.miniSongTitle.setText(song.getTitle());
            binding.miniArtistName.setText(song.getArtist());
            binding.btnPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            
            Glide.with(this)
                    .load(getAlbumArtUri(song.getAlbumId()))
                    .placeholder(R.drawable.ic_default_album_art)
                    .into(binding.miniAlbumArt);
            
            binding.miniPlayer.setVisibility(View.VISIBLE);
        } else {
            binding.miniPlayer.setVisibility(View.GONE);
        }
    }

    private Uri getAlbumArtUri(long albumId) {
        return Uri.parse("content://media/external/audio/albumart/" + albumId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}