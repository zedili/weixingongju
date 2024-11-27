package com.dt.haoyuntong.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.dt.haoyuntong.activity.MainAct

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 处理收到的广播
        handleBroadcast(context, intent)
    }

    private fun handleBroadcast(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            // 应用程序被安装
            // 执行设置App为桌面应用的逻辑
            setAppAsHome(context)
        } else if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            // 应用程序被卸载
            // 执行取消设置App为桌面应用的逻辑
            removeAppAsHome(context)
        }
    }

    private fun setAppAsHome(context: Context) {
        // 将App设置为桌面应用的逻辑
        val pm = context.packageManager
        val componentName = ComponentName(context, MainAct::class.java)

        // 设置App为桌面应用
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 创建一个Intent，用于启动MainActivity
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(homeIntent)
    }

    private fun removeAppAsHome(context: Context) {
        // 取消设置App为桌面应用的逻辑
    }
}