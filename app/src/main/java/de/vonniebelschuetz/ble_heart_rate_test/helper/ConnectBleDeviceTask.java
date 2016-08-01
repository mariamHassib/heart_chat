package de.vonniebelschuetz.ble_heart_rate_test.helper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.fitness.data.BleDevice;

/**
 * Created by niebelschuetz on 15.07.16.
 */

public class ConnectBleDeviceTask extends AsyncTask<BluetoothDevice, Void, String> {

    private final Context context;
    private final BluetoothGattCallback callback;

    public ConnectBleDeviceTask(Context context, BluetoothGattCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(BluetoothDevice... params) {
        params[0].connectGatt(context, true, callback);
        return "success";
    }
}
