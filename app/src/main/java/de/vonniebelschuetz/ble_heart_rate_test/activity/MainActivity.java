package de.vonniebelschuetz.ble_heart_rate_test.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.adapter.ChatRoomsAdapter;
import de.vonniebelschuetz.ble_heart_rate_test.app.Config;
import de.vonniebelschuetz.ble_heart_rate_test.app.EndPoints;
import de.vonniebelschuetz.ble_heart_rate_test.app.MyApplication;
import de.vonniebelschuetz.ble_heart_rate_test.app.Utils;
import de.vonniebelschuetz.ble_heart_rate_test.helper.SimpleDividerItemDecoration;
import de.vonniebelschuetz.ble_heart_rate_test.model.ChatRoom;
import de.vonniebelschuetz.ble_heart_rate_test.model.Message;
import de.vonniebelschuetz.ble_heart_rate_test.model.User;
import de.vonniebelschuetz.ble_heart_rate_test.service.GcmIntentService;


/**
 * Created by niebelschuetz on 05.04.16.
 *
 * Main activity of the app. Displays a list of chat rooms, manages GCM connection and is based on
 * BleActivity
 *
 * @see de.vonniebelschuetz.ble_heart_rate_test.activity.BleActivity
 *
 */
//mariam adding
public class MainActivity extends BleActivity {

    private BleBroadcastReceiver mMainActivityReceiver = new BleBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            switch (intent.getAction()) {
                case Config.REGISTRATION_COMPLETE: {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    String token = intent.getStringExtra("token");
                    Toast.makeText(getApplicationContext(), "GCM registration token: " + token, Toast.LENGTH_LONG).show();
                    break;
                }
                case Config.SENT_TOKEN_TO_SERVER: {
                    // gcm registration id is stored in our server's MySQL
                    Toast.makeText(getApplicationContext(), "GCM registration token is stored in server!", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    };

    /*--- Service handling ---*/

    /*-+- GCM -+-*/
    private ArrayList<ChatRoom> chatRoomArrayList;
    private ChatRoomsAdapter mAdapter;
    /*--- GCM ---*/

    /*-+- Android methods -+-*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for login.  If not logged in launch login activity
        if (MyApplication.getInstance().getPrefManager().getUser() == null) {
            launchLoginActivity();
        }

        // set up content
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        setActionBar((Toolbar) findViewById(R.id.toolbar));
        mActionBar = getActionBar();

        chatRoomArrayList = new ArrayList<>();
        mAdapter = new ChatRoomsAdapter(this, chatRoomArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(
                getApplicationContext()
        ));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new ChatRoomsAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new ChatRoomsAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // when chat is clicked, launch full chat thread activity
                ChatRoom chatRoom = chatRoomArrayList.get(position);
                Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
                intent.putExtra(getPackageName() + ".chat_room_id", chatRoom.getId());
                intent.putExtra(getPackageName() + ".chat_room_users", chatRoom.getUsers());
                Log.d(TAG, "Users in Chatroom:");
                for (User user : chatRoom.getUsers()) {
                    Log.d(TAG, user.getName() + ", " + user.getEmail());
                }

                // set chat room name
                String name = Utils.getChatroomName(chatRoom);
                intent.putExtra(getPackageName() + ".name", name);
                Log.d(TAG, getPackageName()+ ".name: " + name);

                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(getApplicationContext(), chatRoomArrayList.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        }));

        // check for Google play services availability
        if (checkPlayServices()) {
            registerGCM();
            // TODO show spinner while network operation is in progress
            fetchChatRooms();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register broadcast receiver for gcm operations
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Config.REGISTRATION_COMPLETE);
        intentFilter.addAction(Config.SENT_TOKEN_TO_SERVER);
        intentFilter.addAction(Config.ACTION_PUSH_NOTIFICATION);

        registerReceiver(mMainActivityReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                MyApplication.getInstance().logout();
                Log.i(TAG, "onOptionsItemSelected( " + item.getTitleCondensed() + " )");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mMainActivityReceiver);
    }
    /*--- Android methods ---*/

    /*-+- custom methods -+-*/

    /**
     * Handles new push notification
     */
    @Override
    void handlePushNotification(Intent intent) {
        int type = intent.getIntExtra("type", -1);

        // if the push is of chat room message
        // simply update the UI unread messages count
        if (type == Config.PUSH_TYPE_CHATROOM) {
            Message message = (Message) intent.getSerializableExtra("message");
            String chatRoomId = intent.getStringExtra("chat_room_id");

            if (message != null && chatRoomId != null) {
                updateRow(chatRoomId, message);
            }
        } else if (type == Config.PUSH_TYPE_USER) {
            // push belongs to user alone
            // just showing the message in a toast
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Updates the chat list unread count and the last message
     */
    private void updateRow(String chatRoomId, Message message) {
        for (ChatRoom cr : chatRoomArrayList) {
            if (cr.getId().equals(chatRoomId)) {
                int index = chatRoomArrayList.indexOf(cr);
                cr.setLastMessage(message.getMessage());
                cr.setUnreadCount(cr.getUnreadCount() + 1);
                chatRoomArrayList.remove(index);
                chatRoomArrayList.add(index, cr);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * fetching the chat rooms by making http call
     */
    private void fetchChatRooms() {
        String endpoint = EndPoints.USER_CHAT_ROOMS.replace("_ID_", MyApplication.getInstance().getPrefManager().getUser().getId());
        StringRequest strReq = new StringRequest(Request.Method.GET,
                endpoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error flag
                    if (!obj.getBoolean("error")) {
                        JSONArray chatRoomsArray = obj.getJSONArray("chat_rooms");
                        for (int i = 0; i < chatRoomsArray.length(); i++) {
                            JSONObject chatRoomObj = (JSONObject) chatRoomsArray.get(i);
                            Log.d(TAG, "chatRoomObj: " + chatRoomObj.toString());
                            ChatRoom cr = new ChatRoom();
                            cr.setId(chatRoomObj.getString("chat_room_id"));
                            cr.setName(chatRoomObj.getString("name"));
                            cr.setLastMessage("");
                            cr.setUnreadCount(0);
                            cr.setTimestamp(chatRoomObj.getString("created_at"));

                            JSONArray usersArray = chatRoomObj.getJSONArray("users");
                            Log.d(TAG, "usersArray: " + usersArray.toString());
                            ArrayList<User> userList = new ArrayList<>();
                            Log.d(TAG, "Fetched Users in Chatroom: ");
                            for (int j = 0; j < usersArray.length(); j++) {
                                JSONObject userObj = (JSONObject) usersArray.get(j);
                                Log.d(TAG, "userObj: " + userObj.toString());
                                User user = new User();
                                user.setId(userObj.getString("id"));
                                user.setName(userObj.getString("name"));
                                user.setEmail(userObj.getString("email"));
                                Log.d(TAG, user.getName() + ", " + user.getEmail());
                                userList.add(user);
                            }
                            cr.setUsers(userList);

                            chatRoomArrayList.add(cr);
                        }

                    } else {
                        // error in fetching chat rooms
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                mAdapter.notifyDataSetChanged();

                // subscribing to all chat room topics
                subscribeToAllTopics();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);
    }

    // subscribing to global topic
    private void subscribeToGlobalTopic() {
        Intent intent = new Intent(this, GcmIntentService.class);
        intent.putExtra(GcmIntentService.KEY, GcmIntentService.SUBSCRIBE);
        intent.putExtra(GcmIntentService.TOPIC, Config.TOPIC_GLOBAL);
        startService(intent);
    }

    // Subscribing to all chat room topics
    // each topic name starts with `topic_` followed by the ID of the chat room
    // Ex: topic_1, topic_2
    private void subscribeToAllTopics() {
        // TODO only subscribe to topics that current user is a member of
        for (ChatRoom cr : chatRoomArrayList) {
            Intent intent = new Intent(this, GcmIntentService.class);
            intent.putExtra(GcmIntentService.KEY, GcmIntentService.SUBSCRIBE);
            intent.putExtra(GcmIntentService.TOPIC, "topic_" + cr.getId());
            startService(intent);
        }
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // starting the service to register with GCM
    private void registerGCM() {
        Intent intent = new Intent(this, GcmIntentService.class);
        intent.putExtra("key", "register");
        startService(intent);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, Utils.REQUEST_PLAY_SERVICES_RESOLUTION)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported. Google Play Services not installed!");
                Toast.makeText(getApplicationContext(), "This device is not supported. Google Play Services not installed!", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }
}
