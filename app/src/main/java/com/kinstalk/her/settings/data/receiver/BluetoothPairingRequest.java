package com.kinstalk.her.settings.data.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Zhigang Zhang on 2017/12/1.
 */

public class BluetoothPairingRequest extends BroadcastReceiver {
    private final String TAG = "BluetoothPairingRequest";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
            // convert broadcast intent into activity intent (same action string)
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR);
            boolean secure = intent.getBooleanExtra(BluetoothDevice.EXTRA_SECURE_PAIRING, false);
            Intent pairingIntent = new Intent();

            pairingIntent.putExtra(BluetoothDevice.EXTRA_SECURE_PAIRING, secure);
            if (type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
                    type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY ||
                    type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);
                pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_KEY, pairingKey);
            }

            Log.d(TAG, "Bluetooth device " + device.getName() + " request paring with type:" + type);
            switch (type) {
                case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                case BluetoothDevice.PAIRING_VARIANT_CONSENT:
                    device.setPairingConfirmation(true);

                    break;

                case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                case BluetoothDevice.PAIRING_VARIANT_PIN:
                case BluetoothDevice.PAIRING_VARIANT_PASSKEY:

                case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
                case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                    //TODO, not supported yet
                    break;
            }
            abortBroadcast();
        }
    }
}
