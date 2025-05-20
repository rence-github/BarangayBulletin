package com.example.barangaybulletin;

import android.os.Parcel;
import android.os.Parcelable;

public class Announcement implements Parcelable {
    private String id;
    private String title;
    private String content;
    private long timestamp;
    private String imageUrl;
    private long eventDate;
    private String imageBase64;
    private boolean favorite;

    public Announcement() {
        this.timestamp = System.currentTimeMillis();
        this.eventDate = System.currentTimeMillis();
        this.imageUrl = "";
        this.imageBase64 = "";
        this.favorite = false;
    }

    public Announcement(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    protected Announcement(Parcel in) {
        id = in.readString();
        title = in.readString();
        content = in.readString();
        timestamp = in.readLong();
        imageUrl = in.readString();
        eventDate = in.readLong();
        imageBase64 = in.readString();
        favorite = in.readByte() != 0;
    }

    public static final Creator<Announcement> CREATOR = new Creator<Announcement>() {
        @Override
        public Announcement createFromParcel(Parcel in) {
            return new Announcement(in);
        }

        @Override
        public Announcement[] newArray(int size) {
            return new Announcement[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeLong(timestamp);
        dest.writeString(imageUrl);
        dest.writeLong(eventDate);
        dest.writeString(imageBase64);
        dest.writeByte((byte) (favorite ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getImageUrl() { return !imageUrl.isEmpty() ? imageUrl : imageBase64; }
    public long getEventDate() { return eventDate; }
    public void setEventDate(long eventDate) { this.eventDate = eventDate; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public boolean hasImage() {
        return (!imageUrl.isEmpty() || !imageBase64.isEmpty());
    }

    public boolean hasEventDate() {
        return eventDate > 0;
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl.startsWith("data:image")) {
            this.imageBase64 = imageUrl;
            this.imageUrl = "";
        } else {
            this.imageUrl = imageUrl;
            this.imageBase64 = "";
        }
    }
}