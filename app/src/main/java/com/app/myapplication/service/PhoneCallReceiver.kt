package com.dt.haoyuntong.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneCallReceiver(val callback:(phone: String?)->Unit): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            // 获取电话状态
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            // 判断电话状态
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // 电话打进来时的动作
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                // 处理电话号码
                callback.invoke(incomingNumber)
            }
        }
    }
}