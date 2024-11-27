package com.dt.haoyuntong.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SmartActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent intentToService = new Intent(context, MyAccessibilityService.class);
        intentToService.setAction(action);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            intentToService.putExtras(extras);
        }
        context.startService(intentToService);
    }
}
