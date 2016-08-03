package de.vonniebelschuetz.ble_heart_rate_test.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.model.ChatRoom;
import de.vonniebelschuetz.ble_heart_rate_test.model.User;

/**
 * Created by niebelschuetz on 05.04.16.
 *
 * Storage class for static values and methods
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    /* GCM API Keys*/
    protected static final String GCM_API_KEY = "AIzaSyAlgqgcvyG_ncYxCEhy9XklZlx-bq_DW1s";
    protected static final String GCM_SERVER_API_KEY = "AIzaSyAVFG8LpsbnwYhB89N4uQBgnLTssvWhRsU";
    protected static final String GCM_SENDER_ID = "279786364012";

    /* request codes */
    public static final int REQUEST_CONNECT_BLE = 1;
    public static final int REQUEST_PLAY_SERVICES_RESOLUTION = 9000;

    // how long a bluetooth scan should last at most
    public static final long SCAN_PERIOD = 10000;
    /* keys for intent extras */
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    public static final String EXTRA_HEART_RATE_VALUE = "heart_rate_value";

    /* custom intent actions */
    public static final String REQUEST_DISCONNECT_SENSOR= "de.vonniebelschuetz.ble.disconnect_sensor";
    static final String ACTION_DEVICE_CONNECTED = "de.vonniebelschuetz.ble.device_connected";
    public static final String ACTION_HEART_RATE_UPDATED = "de.vonniebelschuetz.ble.hr_updated";
    public static final String ACTION_DEVICE_DISCONNECTED = "de.vonniebelschuetz.ble.device_disconnected";
    public static final String ACTION_PREFERENCE_CHANGED = "de.vonniebelschuetz.ble_heart_rate_test.preference_changed";
    public static final String ACTION_SERVICES_DISCOVERED = "de.vonniebelschuetz.ble.services_discovered";
    public static final String ACTION_DEVICE_CONNECTION_FAILED = "de.vonniebelschuetz.ble.device_connection_failed";

    /* Gatt UUIDs */
    public static final String HEART_RATE_SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB";
    public static final String HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID = "00002A37-0000-1000-8000-00805F9B34FB";
    public static final String CLIENT_CONFIGURATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB";
    private static StandardPBEStringEncryptor sEncryptor;


    // shorthand for getColor(input, 50, 180, 0.0, 0.45, true);
    public static int getColor(int input) {

        // set input range
        int input_end = 188;
        int input_start = 60;

        // set offset for hue
        double offset = 0.0;


        // set scale factor to limit hue range
        double scale = 0.45;
        return getColor(input, input_start, input_end, offset, scale, true);
    }

    // TODO consolidate with Encryptor in ChatRoomActivity
    // TODO do not do this for every decryption, only on password change
    private static void initEncryptor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());

        // set up new encryptor
        sEncryptor = new StandardPBEStringEncryptor();
        sEncryptor.setAlgorithm("PBEwithMD5andDES");
        sEncryptor.setKeyObtentionIterations(1);


        // set encryption password
        sEncryptor.setPassword(preferences.getString(
                MyApplication.getInstance().getResources().getString(R.string.pref_key_encryption_password),
                MyApplication.getInstance().getResources().getString(R.string.pref_default_encryption_password)));
    }

    public static String decryptMessage(String encryptedMessage) {
        initEncryptor();
        String decryptedMessage;
        try {
            decryptedMessage = sEncryptor.decrypt(encryptedMessage);
        } catch (EncryptionOperationNotPossibleException e) {
            Log.e(TAG, "Error decrypting message, not encrypted?", e);
            decryptedMessage = encryptedMessage;
        }
        return decryptedMessage;
    }

    /**
     *
     * get a color in argb format mapped to an int heart rate value in a range from 60 to 180
     * maping is done using hsv for better colors, but converted to rgb for display
     *
     * see http://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
     *
     * @param input heart rate value to map a color to
     * @return the argb color value
     */
    public static int getColor(int input, int input_start, int input_end, double offset, double scale, boolean reverse) {
        // ensure input_max > input_min
        int input_min = Math.min(input_end, input_start);
        int input_max = Math.max(input_end, input_start);
        System.out.println("start is " + input_start + "end is"+ input_end);
        input = ensureRange(input, input_min, input_max);

        // set up input range
        int input_range = input_max - input_min;

        // ensure -360 < offset < 360
        offset = Math.min(Math.max(offset, -360), 360);

        // ensure 0 <= scale < 1
        scale = Math.abs(scale % 1);

        // limit input values to range
        input = Math.min(Math.max(input, input_min), input_max);

        // calculate mapped value in [0.0, 1.0]
        double output = (input - input_min);
        output = output / input_range;
        output = output * 360;
        output = output + offset;

        // if offset pushed value outside range, put it back in
        if (output < 0) {
            output = 360 + output;
        } else if (output > 360) {
            output = output - 360;
        }


        output = output * scale;


        if (reverse) {
            output = -output + (360 * scale);
        }

        double H = output; // Hue (note 0.4 = Green, 0.0 = Red)
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness
        System.out.println("output color is " + output);

        float[] hsv = {(float) H, (float) S, (float) B};

        return Color.HSVToColor(hsv);
    }

    // limit int to range
    private static int ensureRange(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * return name of the other user if there are only 2 users in a chat room,
     * otherwise return the chat rooms own name
     * @param chatRoom the chat room to check
     * @return String: the name to be displayed
     */
    public static String getChatroomName(ChatRoom chatRoom) {
        User myUser = MyApplication.getInstance().getPrefManager().getUser();
        String returnValue = chatRoom.getName();
        boolean myUserContained = false;
        for (User user : chatRoom.getUsers()) {
            if (user.getId().equals(myUser.getId())) {
                myUserContained = true;
            }
        }

        if (chatRoom.getUsers().size() <= 2 && myUserContained) {
            for (User u : chatRoom.getUsers()) {
                if (!(u.getId().equals(myUser.getId()))) {
                    returnValue = u.getName();
                }
            }
        }
        return returnValue;
    }
}
