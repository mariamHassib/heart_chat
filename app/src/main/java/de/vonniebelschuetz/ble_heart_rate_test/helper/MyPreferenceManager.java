package de.vonniebelschuetz.ble_heart_rate_test.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.app.MyApplication;
import de.vonniebelschuetz.ble_heart_rate_test.model.User;

/**
 * Created by niebelschuetz on 17/04/16.
 */
public class MyPreferenceManager {

    private static final String KEY_HR = "heart_rate_value";
    private final Context mContext;
    private String TAG = MyPreferenceManager.class.getSimpleName();

    // Shared Preferences
    private SharedPreferences pref;

    // Sharedpref file name
    private static final String PREF_NAME = "niebelschuetz_gcm";

    // All Shared Preferences Keys
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_USER_AGE = "user_age";
    private static final String KEY_NOTIFICATIONS = "notifications";

    // Constructor
    public MyPreferenceManager(Context context) {
        this.mContext = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }


    public void storeUser(User user) {
        Editor editor = pref.edit();
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_GENDER, user.getGender());
        editor.putString(KEY_USER_AGE, user.getAge());
        editor.apply();

        Log.e(TAG, "User is stored in shared preferences. " + user.getName() + ", " + user.getEmail()
                + ","+ user.getAge() +","+user.getGender());
    }

    public User getUser() {
        if (pref.getString(KEY_USER_ID, null) != null) {
            String id, name, email,gender,age;
            id = pref.getString(KEY_USER_ID, null);
            name = pref.getString(KEY_USER_NAME, null);
            email = pref.getString(KEY_USER_EMAIL, null);
            gender = pref.getString(KEY_USER_GENDER, null);
            age = pref.getString(KEY_USER_AGE, null);

            return new User(id, name, email,gender,age);
        }
        return null;
    }

    public void addNotification(String notification) {
        Editor editor = pref.edit();

        // get old notifications
        String oldNotifications = getNotifications();

        if (oldNotifications != null) {
            oldNotifications += "|" + notification;
        } else {
            oldNotifications = notification;
        }

        editor.putString(KEY_NOTIFICATIONS, oldNotifications);
        editor.apply();
    }

    public String getNotifications() {
        return pref.getString(KEY_NOTIFICATIONS, null);
    }

    public void clear() {
        Editor editor = pref.edit();
        editor.clear();
        editor.apply();
    }
}
