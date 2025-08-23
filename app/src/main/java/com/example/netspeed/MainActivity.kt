package com.example.netspeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.netspeed.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startSpeedService()
            } else {
                Snackbar.make(
                    binding.root,
                    "Notification permission is required to show speed in the status bar.",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.serviceSwitch.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.serviceSwitch.isChecked = SpeedMonitorService.isRunning

        val displayMode = sharedPreferences.getInt("display_mode", R.id.radio_both)
        binding.displayModeGroup.check(displayMode)

        val unit = sharedPreferences.getInt("unit", R.id.radio_MBs)
        binding.unitGroup.check(unit)
    }

    private fun setupListeners() {
        binding.serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionsAndStartService()
            } else {
                stopSpeedService()
            }
        }

        binding.displayModeGroup.setOnCheckedChangeListener { group, checkedId ->
            if(group.findViewById<android.widget.RadioButton>(checkedId).isChecked) {
                sharedPreferences.edit().putInt("display_mode", checkedId).apply()
            }
        }

        binding.unitGroup.setOnCheckedChangeListener { group, checkedId ->
            if(group.findViewById<android.widget.RadioButton>(checkedId).isChecked) {
                sharedPreferences.edit().putInt("unit", checkedId).apply()
            }
        }
    }

    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startSpeedService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Snackbar.make(
                        binding.root,
                        "Notification permission is required to show speed in the status bar.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Grant") {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.show()
                     binding.serviceSwitch.isChecked = false
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startSpeedService()
        }
    }

    private fun startSpeedService() {
        val intent = Intent(this, SpeedMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSpeedService() {
        val intent = Intent(this, SpeedMonitorService::class.java)
        stopService(intent)
    }
}
