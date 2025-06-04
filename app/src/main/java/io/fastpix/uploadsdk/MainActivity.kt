package io.fastpix.uploadsdk

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.fastpix.uploadsdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        _binding.pushVideo.setOnClickListener {
            val chunks = _binding.numberOfChunks.text.toString()

            val intent = Intent(this, PushVideoActivity::class.java)
            if (!chunks.isNullOrEmpty()) {
                intent.putExtra("chunks", chunks.toLong())
            }
            startActivity(intent)
        }
    }
}