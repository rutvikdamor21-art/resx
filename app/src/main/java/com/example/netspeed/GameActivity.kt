package com.example.netspeed

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.netspeed.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gameMode = intent.getStringExtra(MainActivity.EXTRA_GAME_MODE)
        if (gameMode != null) {
            binding.airHockeyView.setGameMode(gameMode)
        }
    }
}
