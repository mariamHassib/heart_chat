package de.vonniebelschuetz.ble_heart_rate_test.activity;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;
import android.widget.BaseAdapter;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.app.EndPoints;
import de.vonniebelschuetz.ble_heart_rate_test.app.MyApplication;
import de.vonniebelschuetz.ble_heart_rate_test.app.Utils;
import de.vonniebelschuetz.ble_heart_rate_test.service.BleService;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
// TODO broadcast on preference change
public class SettingsActivity extends PreferenceActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private static BleService mService;
    private boolean mBound;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected()");
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (!mService.initialize()) {
                android.util.Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            android.util.Log.i(TAG, "Service successfully connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected()");
            mService = null;
            mBound = false;
        }
    };

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            Log.d(TAG, "New value: " + stringValue);

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                String oldValue = String.valueOf(listPreference.getEntry());
                Log.d(TAG, "Old value: " + oldValue);

                // send log request to server
                int index = listPreference.findIndexOfValue(stringValue);
                CharSequence[] entries = listPreference.getEntries();
                String userId = MyApplication.getInstance().getPrefManager().getUser().getId();
                final String logMessage = oldValue + "->" + stringValue;
                String endPoint = EndPoints.LOG.replace("_ID_", userId);
                StringRequest strReq = new StringRequest(Request.Method.POST, endPoint, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e(TAG, "response: " + response);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                JSONObject logEntryObj = obj.getJSONObject("log_entry");
                                Log.i(TAG, "Response: " + logEntryObj.toString());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "json parsing error: " + e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + new String(networkResponse.data));
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("message", logMessage);
                        Log.i(TAG, "Params: " + params.toString());
                        return params;
                    }
                };
                MyApplication.getInstance().addToRequestQueue(strReq);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void setupNotification(Preference preference) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        // Display the fragment as the menu_main content.
        getFragmentManager().beginTransaction().replace(R.id.settings_content,
                new GeneralPreferenceFragment()).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // bind to BleService
        Intent bleServiceIntent = new Intent(this, BleService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @SuppressWarnings("WeakerAccess")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        private final String TAG = getClass().getSimpleName();
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Utils.ACTION_HEART_RATE_UPDATED: {
                        Log.i(TAG, "onReceive(ACTION_HEART_RATE_UPDATED)");
                        int hr = intent.getIntExtra(Utils.EXTRA_HEART_RATE_VALUE, -1);

                        // save hr in shared preferences
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        preferences.edit().putString(getResources().getString(R.string.pref_key_heart_rate), String.valueOf(hr)).apply();
                    }
                }
            }
        };
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            bindPreferenceSummaryToValue(findPreference("display_variants_list"));
            setupNotification(findPreference("display_variants_list"));


            Preference resting_pulse_preference = findPreference(getString(R.string.pref_key_resting_pulse));
            resting_pulse_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // register broadcast receiver to make sure hr values are being stored in shared prefs
                    getActivity().registerReceiver(mReceiver, new IntentFilter(Utils.ACTION_HEART_RATE_UPDATED));
                    // if no sensor connected
                    //   show alert and cancel action
                    // TODO get connection state from service
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    int connection_state = mService.getConnectionState();

                    if (connection_state == BleService.STATE_DISCONNECTED) {
                        Toast.makeText(getActivity(), "Connect a heart rate sensor first!", Toast.LENGTH_LONG).show();
                        return false;
                    } else {
                        String hr = preferences.getString(getResources().getString(R.string.pref_key_heart_rate), "no heart rate");
                        preference.getEditor().putString(getResources().getString(R.string.pref_key_resting_pulse), hr);
                        if (!hr.equals("no heart rate")) {
                            preference.setSummary(hr + " bpm");
                        } else {
                            preference.setSummary(getResources().getString(R.string.pref_summary_resting_pulse));
                        }
                    }
                    // else save hr value as new resting pulse
                    return false;
                }
            });

            // Set age
            Preference user_age_preference = findPreference(getString(R.string.pref_key_user_age));
            user_age_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {


                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    String age = preferences.getString(getResources().getString(R.string.pref_key_user_age), "user_age");
                    preference.getEditor().putString(getResources().getString(R.string.pref_key_user_age), age);
                    preference.setSummary(age + " years");
                    ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();

                   //preference.setSummary(age + " years");

                    return true;
                }
            });





        };

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
