package de.vonniebelschuetz.ble_heart_rate_test.app;

/**
 * Created by niebelschuetz on 20/04/16.
 */
public class EndPoints {

    // localhost url
    // public static final String BASE_URL = "http://192.168.0.101/gcm_chat/v1";

    public static final String BASE_URL = "http://gcm.cisaria.de";
    public static final String LOGIN = BASE_URL + "/user/login";
    public static final String LOG = BASE_URL + "/users/_ID_/log";
    public static final String USER = BASE_URL + "/user/_ID_";
    public static final String CHAT_ROOMS = BASE_URL + "/chat_rooms";
    public static final String USER_CHAT_ROOMS = BASE_URL + "/users/_ID_/chat_rooms";
    public static final String CHAT_THREAD = BASE_URL + "/chat_rooms/_ID_";
    public static final String CHAT_ROOM_MESSAGE = BASE_URL + "/chat_rooms/_ID_/message";
}
