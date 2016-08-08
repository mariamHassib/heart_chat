package de.vonniebelschuetz.ble_heart_rate_test.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.amulyakhare.textdrawable.TextDrawable;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.app.Config;
import de.vonniebelschuetz.ble_heart_rate_test.app.Utils;
import de.vonniebelschuetz.ble_heart_rate_test.adapter.ChatRoomThreadAdapter;
import de.vonniebelschuetz.ble_heart_rate_test.app.EndPoints;
import de.vonniebelschuetz.ble_heart_rate_test.app.MyApplication;
import de.vonniebelschuetz.ble_heart_rate_test.model.Message;
import de.vonniebelschuetz.ble_heart_rate_test.model.User;


/**
 * Created by niebelschuetz on 21/04/16.
 * Activity displaying messages in a single chat room. Based on BleActivity.
 *
 * @see de.vonniebelschuetz.ble_heart_rate_test.activity.BleActivity
 */

// TODO throw out unneccessary code that is now handled by BleActivity
public class ChatRoomActivity extends BleActivity {
    private StandardPBEStringEncryptor mEncryptor;
    private int messageCount=-1;
    private String chatRoomId;
    private RecyclerView recyclerView;
    private ChatRoomThreadAdapter mMessageAdapter;
    private ArrayList<Message> messageArrayList;
    private BleBroadcastReceiver mChatRoomActivityReceiver = new BleBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            switch (intent.getAction()) {
                case Utils.ACTION_PREFERENCE_CHANGED: {
                    String key = intent.getStringExtra("key");
                    if (key.equals(getResources().getString(R.string.pref_key_encryption_password))) {
                        initEncryptor();
                    }
                    break;
                }
                default: {
                    Log.i(TAG, "onReceive( UNKNOWN ACTION )");
                }
            }
        }
    };
    private EditText inputMessage;
    private ArrayList<User> chatRoomUsers;
    private int lastHrReceived = 0;
    private int receivedAge = 0;

    private void initEncryptor() {
        // set up encryptor
        if (mEncryptor == null) {
            mEncryptor = new StandardPBEStringEncryptor();
            mEncryptor.setAlgorithm("PBEwithMD5andDES");
            mEncryptor.setKeyObtentionIterations(1);
        }
        // set encryption password
        mEncryptor.setPassword(mPreferences.getString(
                getResources().getString(R.string.pref_key_encryption_password),
                getResources().getString(R.string.pref_default_encryption_password)));
    }

    private String decryptMessage(String encryptedMessage) {
        String decryptedMessage;
        try {
            decryptedMessage = mEncryptor.decrypt(encryptedMessage);
        } catch (EncryptionOperationNotPossibleException e) {
            Log.e(TAG, "Error decrypting message, not encrypted?", e);
            decryptedMessage = encryptedMessage;
        }
        return decryptedMessage;
    }
Context mainContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        mActionBar = getActionBar();
     //  mainContext=this.getApplicationContext();

        inputMessage = (EditText) findViewById(R.id.message);
        Button btnSend = (Button) findViewById(R.id.btn_send);
        Button btnSendHr = (Button) findViewById(R.id.btn_send_hr);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // set up encryptor with password from shared preferences
        initEncryptor();

        // TODO get mode string from string array values in R
        if (retrieveMode().equals("button") && btnSendHr != null) {
            btnSendHr.setVisibility(View.VISIBLE);
        } else if (btnSendHr != null){
            btnSendHr.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        chatRoomId = intent.getStringExtra(getPackageName() + ".chat_room_id");
        try {
            chatRoomUsers = (ArrayList<User>) intent.getSerializableExtra(getPackageName() + ".chat_room_users");
        } catch (ClassCastException e) {
            Log.e(TAG, "Error casting users", e);
            chatRoomUsers = new ArrayList<>();
        }
        String title = intent.getStringExtra(getPackageName() + ".name");

        if (mActionBar != null) {
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setTitle(title);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e(TAG, "Action bar is null!");
        }

        if (chatRoomId == null) {
            Toast.makeText(getApplicationContext(), "Chat room not found!", Toast.LENGTH_SHORT).show();
           finish();
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
        }

        messageArrayList = new ArrayList<>();

        // self user id is to identify the message owner
        String selfUserId = MyApplication.getInstance().getPrefManager().getUser().getId();

        mMessageAdapter = new ChatRoomThreadAdapter(this, messageArrayList, selfUserId);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mMessageAdapter);

        if (btnSend != null) {
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage();
                }
            });
        }

        if (btnSendHr != null) {
            btnSendHr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // save hr in shared preferences
                    // save hr in shared preferences

                    String hr = mPreferences.getString(getResources().getString(R.string.pref_key_heart_rate), "no heart rate");
                    String message = "My heart rate now is: " + hr +" bpm";
                    sendMessage(message, hr);
                }
            });
        }

        fetchChatThread();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        super.onStart();

        // registering the receiver for new notifications
        registerReceiver(mChatRoomActivityReceiver, new IntentFilter(Utils.ACTION_HEART_RATE_UPDATED));
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
        //String msg = getIntent().getStringExtra(Config.PUSH_TYPE_USER);
        //inputMessage.setText(msg);
        // invalidate adapter data set, since preference may have changed
        // - might need to reset the color of all messages
        // TODO get the color to update when switching back from history mode
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mChatRoomActivityReceiver);
        super.onStop();
    }

    void resetHearRateViews() {
        if (mActionBar != null && retrieveMode().equals("numeric")) {
            mActionBar.setSubtitle("no heart rate");
        }
    }

    private String retrieveMode() {
        return mPreferences.getString(getResources().getString(R.string.pref_key_display_mode_list),
                getString(R.string.pref_default_mode));
    }

    private void sendMessage() {
         String message ="";
        //If this is the first message in the chat, send age and resting HR
        if (messageCount==-1){

             message= this.inputMessage.getText().toString().trim()
                    +"__"+mPreferences.getString(getResources().getString(R.string.pref_key_user_age),"26")
                    +','+mPreferences.getString(getResources().getString(R.string.pref_key_resting_pulse),"60")
                    +"__";
            messageCount++;
        }
        else{
         message = this.inputMessage.getText().toString().trim();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // TODO, FIXME this throws a class cast exception on fresh installs that have not yet read a heart rate
        String hr = preferences.getString(getResources().getString(R.string.pref_key_heart_rate), "-1");
        sendMessage(message, hr);
    }

    /**
     * Posting a new message in chat room
     * will make an http call to our server. Our server again sends the message
     * to all the devices as push notification
     * */
    private void sendMessage(final String message, final String hr) {

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // encrypt the message
        final String encryptedText = mEncryptor.encrypt(message);


        String endPoint = EndPoints.CHAT_ROOM_MESSAGE.replace("_ID_", chatRoomId);

        Log.e(TAG, "endpoint: " + endPoint);

        // reset the text edit field
        this.inputMessage.setText("");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.i(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (!obj.getBoolean("error")) {
                        JSONObject commentObj = obj.getJSONObject("message");

                        String commentId = commentObj.getString("message_id");
                        int heartRate = -2;
                        if (commentObj.has("heart_rate")) {
                            heartRate = commentObj.getInt("heart_rate");
                        } else {
                            Log.d(TAG, "no heart rate in response object");
                        }
                        String commentText = commentObj.getString("message");
                        String createdAt = commentObj.getString("created_at");

                        JSONObject userObj = obj.getJSONObject("user");
                        String userId = userObj.getString("user_id");
                        String userName = userObj.getString("name");
                        User user = new User(userId, userName, null);


                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(decryptMessage(commentText));
                        message.setCreatedAt(createdAt);
                        message.setUser(user);
                        message.setHr(heartRate);

                        messageArrayList.add(message);

                        mMessageAdapter.notifyDataSetChanged();
                        if (mMessageAdapter.getItemCount() > 1) {
                            // scrolling to bottom of the recycler view
                            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mMessageAdapter.getItemCount() - 1);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                inputMessage.setText(message);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", MyApplication.getInstance().getPrefManager().getUser().getId());
                params.put("message", encryptedText);
                params.put("mode", retrieveMode());
                params.put("heart_rate", hr);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            }
        };


        // disabling retry policy so that it won't make
        // multiple http calls
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        strReq.setRetryPolicy(policy);

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);
    }

    @Override
    void updateHeartRateViews(String hr) {
        switch (retrieveMode()) {
            case "numeric":
                super.updateHeartRateViews(hr);
            case "moodlight":
                // set up TextDrawable builder
                TextDrawable.IBuilder builder = TextDrawable.builder()
                        .beginConfig()
                        .toUpperCase()
                        .useFont(Typeface.DEFAULT)
                        .textColor(Color.WHITE)
                        .endConfig()
                        .round();
                String firstInitial = MyApplication.getInstance().getPrefManager().getUser().getName().substring(0,1);
                String secondInitial = "";
                if (chatRoomUsers.size() <= 2) {
                    Log.d(TAG, chatRoomUsers.size() + " User(s) in Chat:");
                    for (User user : chatRoomUsers) {
                        String userName = user.getName();
                        Log.d(TAG, "User: " + userName);

                        if (!userName.equals(MyApplication.getInstance().getPrefManager().getUser().getName())) {
                            secondInitial = userName.substring(0,1);
                        }
                    }
                } else {
                    secondInitial = "+" + String.valueOf(chatRoomUsers.size()-1);
                }

                String resting_pulse = mPreferences.getString(getResources().getString(R.string.pref_summary_resting_pulse), "60");
                int max_hr= (int)(208- (0.7*(Integer.parseInt(mPreferences.getString(getString(R.string.pref_key_user_age), "26")))));
                int maxhradjusted = (int)(max_hr-(max_hr-(0.9*max_hr)));
                int firstColor = Utils.getColor(Integer.parseInt(hr), Integer.parseInt(resting_pulse), (int)(max_hr-(max_hr-(0.9*max_hr))), 0.0, 0.30, true);
                Log.i(TAG, "max HR is now" + maxhradjusted);
                //Toast.makeText(getApplicationContext(), "" +  "max HR is now" + max_hr,Toast.LENGTH_SHORT).show();;

                //Make circle grey if last received is not more than 0
                float[] hsv = {0, 0, 0.78f};
                int secondColor=Color.HSVToColor(hsv);

                if(lastHrReceived>0) {
                    int secondAge = mMessageAdapter.getUserAge();
                    int secondUserMaxHr= 208-(int)(0.7*secondAge);
                    int secondUserRHR = mMessageAdapter.getUserRestPulse();
                    secondColor = Utils.getColor(lastHrReceived, secondUserRHR, (int)(secondUserMaxHr - (secondUserMaxHr - ( 0.9 * secondUserMaxHr))), 0.0, 0.30, true);
                }
                TextDrawable firstDrawable = builder.build(firstInitial, firstColor);
                TextDrawable secondDrawable = builder.build(secondInitial, secondColor);

                // set the colored circle representing the mood
                ImageView firstInitialsView = (ImageView) findViewById(R.id.firstInitialsView);
                if (firstInitialsView != null) {
                    firstInitialsView.setImageDrawable(firstDrawable);
                    firstInitialsView.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "First Initials View not found!", new Resources.NotFoundException("firstInitialsView"));
                }
                ImageView secondInitialsView = (ImageView) findViewById(R.id.secondInitialsView);
                if (secondInitialsView != null) {
                    secondInitialsView.setImageDrawable(secondDrawable);
                    if (!secondInitial.equals("")) {
                        secondInitialsView.setVisibility(View.VISIBLE);
                    } else {
                        Log.e(TAG, "Second Initials empty!");
                    }
                } else {
                    Log.e(TAG, "Second Initials View not found!", new Resources.NotFoundException("secondInitialsView"));
                }
                break;
            case "history":
                // do nothing, this will be handled by the message receiver
            case "button":
                // do nothing, this is handled on button press
                break;
            default:
                Log.e(TAG, "invalid mode selected");
        }
    }

    @Override
    void handlePushNotification(Intent intent) {
        Log.i(TAG, "handlePushNotification(" + intent.toUri(0) + ")");
        Message message = (Message) intent.getSerializableExtra("message");
        message.setMessage(decryptMessage(message.getMessage()));
        String chatRoomId = intent.getStringExtra("chat_room_id");
        if (chatRoomId != null) {
            this.lastHrReceived = message.getHr();
            messageArrayList.add(message);
            mMessageAdapter.notifyDataSetChanged();
            if (mMessageAdapter.getItemCount() > 1) {
                recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mMessageAdapter.getItemCount() - 1);
            }
        }
    }

    /**
     * Fetching all the messages of a single chat room
     * */
    private void fetchChatThread() {

        String endPoint = EndPoints.CHAT_THREAD.replace("_ID_", chatRoomId);
        Log.e(TAG, "endPoint: " + endPoint);

        StringRequest strReq = new StringRequest(Request.Method.GET,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (!obj.getBoolean("error")) {
                        JSONArray commentsObj = obj.getJSONArray("messages");

                        for (int i = commentsObj.length() - 1; i >= 0 ; i--) {
                            JSONObject commentObj = (JSONObject) commentsObj.get(i);

                            String commentId = commentObj.getString("message_id");
                            String commentText = commentObj.getString("message");
                            String createdAt = commentObj.getString("created_at");
                            int heartRate = commentObj.getInt("heart_rate");

                            JSONObject userObj = commentObj.getJSONObject("user");
                            String userId = userObj.getString("user_id");
                            String userName = userObj.getString("username");
                            User user = new User(userId, userName, null);

                            String messageText = decryptMessage(commentText);

                            Message message = new Message();
                            message.setId(commentId);
                            message.setMessage(messageText);
                            message.setCreatedAt(createdAt);
                            message.setUser(user);
                            message.setHr(heartRate);

                            messageArrayList.add(message);
                        }
                        Collections.sort(messageArrayList);

                        mMessageAdapter.notifyDataSetChanged();
                        if (mMessageAdapter.getItemCount() > 1) {
                            recyclerView.getLayoutManager().scrollToPosition(mMessageAdapter.getItemCount() - 1);

                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
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
}
