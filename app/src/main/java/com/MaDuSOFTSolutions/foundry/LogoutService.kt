package com.MaDuSOFTSolutions.foundry

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class LogoutService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY // Don't restart if app is killed
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("LogoutService", "App removed from recent apps, clearing session.")
        val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
