package com.otowave.otowave;

import android.os.Parcel;
import android.os.Parcelable;

public class AudioModel implements Parcelable {
    private final String title;
    private final String artist;
    private final String path;
    private final long duration;
    private final long albumId;

    public AudioModel(String title, String artist, String path, long duration, long albumId) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.albumId = albumId;
    }

    protected AudioModel(Parcel in) {
        title = in.readString();
        artist = in.readString();
        path = in.readString();
        duration = in.readLong();
        albumId = in.readLong();
    }

    public static final Creator<AudioModel> CREATOR = new Creator<AudioModel>() {
        @Override
        public AudioModel createFromParcel(Parcel in) {
            return new AudioModel(in);
        }

        @Override
        public AudioModel[] newArray(int size) {
            return new AudioModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(path);
        dest.writeLong(duration);
        dest.writeLong(albumId);
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }
}