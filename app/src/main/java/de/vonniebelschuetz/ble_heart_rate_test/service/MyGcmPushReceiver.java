package de.vonniebelschuetz.ble_heart_rate_test.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.regex.*;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import de.vonniebelschuetz.ble_heart_rate_test.activity.MainActivity;
import de.vonniebelschuetz.ble_heart_rate_test.activity.ChatRoomActivity;
import de.vonniebelschuetz.ble_heart_rate_test.app.Config;
import de.vonniebelschuetz.ble_heart_rate_test.app.MyApplication;
import de.vonniebelschuetz.ble_heart_rate_test.app.NotificationUtils;
import de.vonniebelschuetz.ble_heart_rate_test.app.Utils;
import de.vonniebelschuetz.ble_heart_rate_test.model.ChatRoom;
import de.vonniebelschuetz.ble_heart_rate_test.model.Message;
import de.vonniebelschuetz.ble_heart_rate_test.model.User;

/**
 * Created by niebelschuetz on 25/04/16.
 */
public class MyGcmPushReceiver extends GcmListenerService {

    private static final String TAG = MyGcmPushReceiver.class.getSimpleName();

    private NotificationUtils notificationUtils;

    String ageAndresthr="";

    /**
     * Called when message is received.
     *
     * @param from   SenderID of the sender.
     * @param bundle Data bundle containing message data as key/value pairs.
     *               For Set of keys use data.keySet().
     */

  //  int requestID = (int) System.currentTimeMillis(); //unique requestID to differentiate between various notification with same NotifId
   // int flags = PendingIntent.FLAG_CANCEL_CURRENT;
    Intent resultIntent;
    Intent mainIntent;
   // PendingIntent pIntent;
    @Override
    public void onMessageReceived(String from, Bundle bundle) {

        resultIntent = new Intent(MyGcmPushReceiver.this, ChatRoomActivity.class);
        mainIntent = new Intent(MyGcmPushReceiver.this, MainActivity.class);
      //  pIntent = PendingIntent.getActivity(this, requestID, intent, flags);
        String title = bundle.getString("title");
        Boolean isBackground = Boolean.valueOf(bundle.getString("is_background"));
        String flag = bundle.getString("flag");
        String data = bundle.getString("data");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "title: " + title);
        Log.d(TAG, "isBackground: " + isBackground);
        Log.d(TAG, "flag: " + flag);
        Log.d(TAG, "data: " + data);

        if (flag == null)
            return;

        if(MyApplication.getInstance().getPrefManager().getUser() == null){
            // user is not logged in, skipping push notification
            Log.e(TAG, "user is not logged in, skipping push notification");
            return;
        }

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        switch (Integer.parseInt(flag)) {
            case Config.PUSH_TYPE_CHATROOM:
                // push notification belongs to a chat room
                processChatRoomPush(title, isBackground, data);
                break;
            case Config.PUSH_TYPE_USER:
                // push notification is specific to user
                processUserMessage(title, isBackground, data);
                break;
        }
    }

    /**
     * Processing chat room push message
     * this message will be broadcasts to all the activities registered
     * */
    /**
     * Processing chat room push message
     * this message will be broadcasts to all the activities registered
     * */
    private void processChatRoomPush(String title, boolean isBackground, String data) {
        Log.i(TAG, "processChatRoomPush(" + title + ", " + String.valueOf(isBackground) +", " + data + ")");
        if (!isBackground) {
       //     if(getApplicationContext()!=null){

            try {
                JSONObject datObj = new JSONObject(data);

                String chatRoomId = datObj.getString("chat_room_id");

                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();
                message.setMessage(Utils.decryptMessage(mObj.getString("message")));
                message.setId(mObj.getString("message_id"));
                message.setHr(mObj.getInt("heart_rate"));
                message.setCreatedAt(mObj.getString("created_at"));

                JSONObject uObj = datObj.getJSONObject("user");

                // skip the message if the message belongs to same user as
                // the user would be having the same message when he was sending
                // but it might differs in your scenario
                if (uObj.getString("user_id").equals(MyApplication.getInstance().getPrefManager().getUser().getId())) {
                    Log.e(TAG, "Skipping the push message as it belongs to same user");
                    return;
                }

                User user = new User();
                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);

                // Check if this message contains age and resting pulse as such text_age,pulse_
                String messageString = message.getMessage();
                String regexString = Pattern.quote("__Start__") + "(.*?)" + Pattern.quote("__End__");
                Pattern pattern = Pattern.compile(regexString);
                // text contains the full text that you want to extract data
                Matcher matcher = pattern.matcher(messageString);

                if (matcher.find()) {
                    ageAndresthr = matcher.group(1); // Since (.*?) is capturing group 1
                    Log.i(TAG, "age and rest hr are: "+ ageAndresthr);
                    message.setMessage(messageString.substring(0, messageString.length() - 21));
                    Log.i(TAG, "message now is: " + message.getMessage());

                }

                // verifying whether the app is in background or foreground
                //
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                //if(getApplicationContext()!=null){
                    Log.d(TAG, "App is not in background");

                    // app is in foreground, broadcast the push message
                    Intent pushNotification = new Intent(Config.ACTION_PUSH_NOTIFICATION);
                    pushNotification.putExtra("type", Config.PUSH_TYPE_CHATROOM);
                    pushNotification.putExtra("message", message);
                    pushNotification.putExtra("chat_room_id", chatRoomId);
                    sendBroadcast(pushNotification);

                    // play notification sound
                    NotificationUtils notificationUtils = new NotificationUtils();
                    notificationUtils.playNotificationSound();
                } else {
                    Log.d(TAG, "App is in background");
                    // app is in background. show the message in notification try
                     //mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                   resultIntent.putExtra("chat_room_id", chatRoomId);
                    ArrayList<User> users= new ArrayList<User>();
                    users.add(user);
                    resultIntent.putExtra( "chat_room_users", users);
                    resultIntent.putExtra("name", user.getName());
                    showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                }

            } catch (JSONException e) {
                Log.e(TAG, "json parsing error: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            // the push notification is silent, may be other operations needed
            // like inserting it in to SQLite
        }
    }

    /**
     * Processing user specific push message
     * It will be displayed with / without image in push notification tray
     * */
    private void processUserMessage(String title, boolean isBackground, String data) {
        if (!isBackground) {

            try {
                JSONObject datObj = new JSONObject(data);

                String imageUrl = datObj.getString("image");

                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();
                message.setMessage(Utils.decryptMessage(mObj.getString("message")));
                message.setId(mObj.getString("message_id"));
                message.setCreatedAt(mObj.getString("created_at"));
                message.setHr(datObj.getInt("heart_rate"));

                JSONObject uObj = datObj.getJSONObject("user");
                User user = new User();
                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);

                // Check if this message contains age and resting pulse as such text_age,pulse_
                String messageString = message.getMessage();
                String regexString = Pattern.quote("__Start__") + "(.*?)" + Pattern.quote("__End__");
                Pattern pattern = Pattern.compile(regexString);
                // text contains the full text that you want to extract data
                Matcher matcher = pattern.matcher(messageString);

                if (matcher.find()) {
                    ageAndresthr = matcher.group(1); // Since (.*?) is capturing group 1
                    Log.i(TAG, "age and rest hr are: "+ ageAndresthr);
                    message.setMessage(messageString.substring(0, messageString.length() - 21));
                    Log.i(TAG, "message now is: " + message.getMessage());

                }

                // verifying whether the app is in background or foreground
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {

                    // app is in foreground, broadcast the push message
                    Intent pushNotification = new Intent(Config.ACTION_PUSH_NOTIFICATION);
                    pushNotification.putExtra("type", Config.PUSH_TYPE_USER);
                    pushNotification.putExtra("message", message);
                    sendBroadcast(pushNotification);

                    // play notification sound
                    NotificationUtils notificationUtils = new NotificationUtils();
                    notificationUtils.playNotificationSound();
                } else {

                    // app is in background. show the message in notification try
                  //  Intent resultIntent = new Intent(getApplicationContext(), ChatRoomActivity.class);

                    // check for push notification image attachment
                    if (TextUtils.isEmpty(imageUrl)) {
                        showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                    } else {
                        // push notification contains image
                        // show it with the image
                        showNotificationMessageWithBigImage(getApplicationContext(), title, message.getMessage(), message.getCreatedAt(), resultIntent, imageUrl);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "json parsing error: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            // the push notification is silent, may be other operations needed
            // like inserting it in to SQLite
            Log.i(TAG, "processUserMessage(isBackground = true)");
        }
    }

    /**
     * Showing notification with text only
     * */
    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }

    /**
     * Showing notification with text and image
     * */
    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
    }
}
