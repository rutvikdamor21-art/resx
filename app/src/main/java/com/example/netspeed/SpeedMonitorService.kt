package com.example.netspeed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class SpeedMonitorService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTime: Long = 0

    private val speedCheckRunnable = object : Runnable {
        override fun run() {
            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()
            val currentTime = System.currentTimeMillis()

            val timeDifference = (currentTime - lastTime) / 1000.0 // in seconds

            if (lastTime != 0L && timeDifference > 0) {
                val rxSpeed = (currentRxBytes - lastRxBytes) / timeDifference // Bytes/sec
                val txSpeed = (currentTxBytes - lastTxBytes) / timeDifference // Bytes/sec

                updateNotification(rxSpeed, txSpeed)
            }

            lastRxBytes = currentRxBytes
            lastTxBytes = currentTxBytes
            lastTime = currentTime

            handler.postDelayed(this, 2000) // Update every 2 seconds
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Internet Speed")
            .setContentText("Calculating...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        handler.post(speedCheckRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(speedCheckRunnable)
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Internet Speed Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(rxSpeed: Double, txSpeed: Double) {
        val displayMode = sharedPreferences.getInt("display_mode", R.id.radio_both)
        val unit = sharedPreferences.getInt("unit", R.id.radio_MBs)

        val downSpeedStr = formatSpeed(rxSpeed, unit)
        val upSpeedStr = formatSpeed(txSpeed, unit)

        val contentText = when (displayMode) {
            R.id.radio_download -> "↓ $downSpeedStr"
            R.id.radio_upload -> "↑ $upSpeedStr"
            else -> "↓ $downSpeedStr | ↑ $upSpeedStr"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Internet Speed")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatSpeed(speedInBytesPerSecond: Double, unit: Int): String {
        val (value, unitLabel) = when (unit) {
            R.id.radio_Mbs -> Pair(speedInBytesPerSecond * 8 / 1_000_000, "Mb/s")
            R.id.radio_KBs -> Pair(speedInBytesPerSecond / 1024, "KB/s")
            R.id.radio_Kbs -> Pair(speedInBytesPerSecond * 8 / 1000, "kb/s")
            else -> Pair(speedInBytesPerSecond / (1024 * 1024), "MB/s") // Default to MB/s
        }

        return String.format("%.2f %s", value, unitLabel)
    }


    companion object {
        const val CHANNEL_ID = "speed_monitor_channel"
        const val NOTIFICATION_ID = 1
        @Volatile var isRunning = false
    }
}
