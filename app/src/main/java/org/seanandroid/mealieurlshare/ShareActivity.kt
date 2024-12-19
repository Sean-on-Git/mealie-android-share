package org.seanandroid.mealieurlshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ShareActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var handler: Handler
    private var isRequestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the shared preferences
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val url = sharedPreferences.getString("url", null)
        val token = sharedPreferences.getString("token", null)

        // Get the shared text
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (url != null && token != null && sharedText != null) {
            sendPostRequest(url, token, sharedText)
        } else {
            Toast.makeText(this, "URL or TOKEN not set", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendPostRequest(url: String, token: String, sharedText: String) {
        isRequestRunning = true
        val fullUrl = "$url/api/recipe/create/url"
        val requestBody = FormBody.Builder()
            .add("data", sharedText)
            .build()

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (isRequestRunning) {
                isRequestRunning = false
                sendNotification("Request timed out")
                finish()
            }
        }, 30000) // 30 seconds timeout

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isRequestRunning = false
                sendNotification("Request failed: ${e.message}")
                finish()
            }

            override fun onResponse(call: Call, response: Response) {
                isRequestRunning = false
                if (response.isSuccessful) {
                    sendNotification("Request successful!")
                } else {
                    sendNotification("Request failed: ${response.message}")
                }
                finish()
            }
        })
    }

    private fun sendNotification(message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mealie_url_share_channel"

        // Create the notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mealie URL Share Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mealie URL Share")
            .setContentText(message)
            .setSmallIcon(R.drawable.baseline_error_24) // Replace with your notification icon
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
