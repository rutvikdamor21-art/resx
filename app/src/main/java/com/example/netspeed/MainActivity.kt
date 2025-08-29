package com.example.netspeed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.netspeed.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EXTRA_GAME_MODE = "game_mode"
        const val GAME_MODE_AI = "ai"
        const val GAME_MODE_PVP = "pvp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.playerVsAiButton.setOnClickListener {
            startGame(GAME_MODE_AI)
        }

        binding.playerVsPlayerButton.setOnClickListener {
            startGame(GAME_MODE_PVP)
        }

    }

    private fun startGame(gameMode: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(EXTRA_GAME_MODE, gameMode)
        }
        startActivity(intent)
    }
}
