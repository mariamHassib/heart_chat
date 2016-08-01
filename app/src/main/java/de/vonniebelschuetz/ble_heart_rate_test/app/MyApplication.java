package de.vonniebelschuetz.ble_heart_rate_test.app;

/**
 * Created by Lincoln on 14/10/15.
 */

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.activity.LoginActivity;
import de.vonniebelschuetz.ble_heart_rate_test.helper.MyPreferenceManager;

import static de.vonniebelschuetz.ble_heart_rate_test.app.Utils.ACTION_DEVICE_CONNECTED;


/**
 * Created by niebelschuetz on 13/04/16.
 */

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class
            .getSimpleName();

    private RequestQueue mRequestQueue;

    private static MyApplication mInstance;

    private MyPreferenceManager pref;

    // Broadcast receiver to handle application wide events
    private BroadcastReceiver mApplicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                default: {
                    Log.e(TAG, "onReceive( UNKNOWN ACTION )");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @SuppressWarnings("WeakerAccess")
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public MyPreferenceManager getPrefManager() {
        if (pref == null) {
            pref = new MyPreferenceManager(this);
        }

        return pref;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public void logout() {
        pref.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
