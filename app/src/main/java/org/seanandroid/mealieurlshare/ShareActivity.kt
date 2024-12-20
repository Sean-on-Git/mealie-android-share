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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ShareActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var handler: Handler
    private var isRequestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DatabaseHelper(this)
        dbHelper.open()

        // Get the shared text
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        // pull variables from our app's SQLite DB
        val url = dbHelper.getUrl()
        val token = dbHelper.getToken()

        if (url != null && token != null && sharedText != null) {
            sendPostRequest(url, token, sharedText)
        } else {
            Toast.makeText(this, "URL or TOKEN not set", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Closes DB when process closes
    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    private fun sendPostRequest(url: String, token: String, sharedText: String) {
        isRequestRunning = true
        val fullUrl = "$url/api/recipes/create/url"

        // Create JSON object for the request body
        val jsonBody = JSONObject()
        jsonBody.put("includeTags", false) // Set includeTags to false
        jsonBody.put("url", sharedText) // Set the URL to the shared text

        // Create the request body with JSON media type
        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

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
                    // Send a Toast notification to the running UI (if successfully POSTed)
                    runOnUiThread {
                        Toast.makeText(this@ShareActivity, "URL sent to your Mealie API", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    sendNotification("Request failed: ${response.code} ${response.message}")
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
            .setSmallIcon(R.mipmap.mealie_upload_round) // Replace with your notification icon
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
