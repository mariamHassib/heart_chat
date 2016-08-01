package de.vonniebelschuetz.ble_heart_rate_test.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Lincoln on 07/01/16.
 */
public class ChatRoom implements Serializable {
    String id, name, lastMessage, timestamp;
    int unreadCount;
    ArrayList<User> users;

    public ChatRoom() {
    }

    public ChatRoom(String id, String name, String lastMessage, String timestamp, int unreadCount, ArrayList<User> users) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
        this.users = users;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
}
