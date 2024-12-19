package org.seanandroid.mealieurlshare

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.seanandroid.mealieurlshare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission on first install
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        binding.saveButton.setOnClickListener {
            val url = binding.urlEditText.text.toString()
            val token = binding.tokenEditText.text.toString()
            // Save URL and TOKEN in SharedPreferences
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("url", url)
                putString("token", token)
                apply()
            }
            // Show a Toast message to confirm saving
            Toast.makeText(this, "URL and TOKEN saved", Toast.LENGTH_SHORT).show()
        }
    }
}
