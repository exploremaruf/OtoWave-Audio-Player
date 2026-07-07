package com.otowave.otowave;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.otowave.otowave.databinding.SongcardBinding;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private final List<AudioModel> songsList;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AudioModel song, int position);
    }

    public MusicAdapter(List<AudioModel> songsList, Context context, OnItemClickListener listener) {
        this.songsList = songsList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SongcardBinding binding = SongcardBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioModel song = songsList.get(position);
        holder.binding.songTitle.setText(song.getTitle());
        holder.binding.songArtist.setText(song.getArtist());
        holder.binding.songDuration.setText(formatDuration(song.getDuration()));

        Glide.with(context)
                .load(getAlbumArtUri(song.getAlbumId()))
                .placeholder(R.drawable.ic_default_album_art)
                .into(holder.binding.songAlbumArt);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(song, position);
            }
        });
    }

    private Uri getAlbumArtUri(long albumId) {
        return Uri.parse("content://media/external/audio/albumart/" + albumId);
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final SongcardBinding binding;

        public ViewHolder(@NonNull SongcardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private String formatDuration(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
    }
}