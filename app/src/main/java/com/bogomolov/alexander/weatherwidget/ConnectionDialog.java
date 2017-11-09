package com.bogomolov.alexander.weatherwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

/**
 * Created by admin on 09.11.2017.
 */

public class ConnectionDialog extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Connection Problem");
        alertDialog.setMessage("Connect to the Internet");
        alertDialog.setCancelable(true);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        };
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Connect to WIFI", listener);
        alertDialog.show();
    }
}
