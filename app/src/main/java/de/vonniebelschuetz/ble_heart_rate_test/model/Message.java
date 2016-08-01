package de.vonniebelschuetz.ble_heart_rate_test.model;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by niebelschuetz on 12/04/16.
 *
 * Model class for messages
 */
public class Message implements Serializable, Comparable<Message> {
    String id, message, createdAt;
    int hr;
    User user;

    public Message() {
    }

    public Message(String id, String message, String createdAt, int hr, User user) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
        this.hr = hr;
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getHr() {
        return hr;
    }

    public void setHr(int hr) {
        this.hr = hr;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int compareTo(@NonNull Message another) {
        return this.createdAt.compareTo(another.getCreatedAt());
    }
}
