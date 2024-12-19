package org.seanandroid.mealieurlshare

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.seanandroid.mealieurlshare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        dbHelper.open()

        // Request notification permission on first install
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Load text fields from database
        val url = dbHelper.getUrl()
        val token = dbHelper.getToken()

        if (url != null) {
            binding.urlEditText.setText(url)
        }

        if (token != null) {
            binding.tokenEditText.setText(token)
        }

        binding.saveButton.setOnClickListener {
            val newUrl = binding.urlEditText.text.toString()
            val newToken = binding.tokenEditText.text.toString()
            // Save URL and TOKEN in SQLite database
            dbHelper.saveUrlAndToken(newUrl, newToken)
            // Show a Toast message to confirm saving
            Toast.makeText(this, "URL and TOKEN saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mealie_url_share.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "settings"
        private const val COLUMN_URL = "url"
        private const val COLUMN_TOKEN = "token"
    }

    private var db: SQLiteDatabase? = null

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_NAME ($COLUMN_URL TEXT, $COLUMN_TOKEN TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Do nothing
    }

    fun open() {
        db = writableDatabase
    }

    override fun close() {
        db?.close()
    }

    fun saveUrlAndToken(url: String, token: String) {
        open()
        val previousUrl = getUrl()
        val previousToken = getToken()

        if (url != previousUrl || token != previousToken) {
            db?.execSQL("DELETE FROM $TABLE_NAME")
            db?.execSQL("INSERT INTO $TABLE_NAME ($COLUMN_URL, $COLUMN_TOKEN) VALUES ('$url', '$token')")
        }
    }

    fun getUrl(): String? {
        open()
        val cursor = db?.rawQuery("SELECT $COLUMN_URL FROM $TABLE_NAME", null)
        var url: String? = null
        if (cursor?.moveToFirst() == true) {
            url = cursor.getString(0)
        }
        cursor?.close()
        return url
    }

    fun getToken(): String? {
        open()
        val cursor = db?.rawQuery("SELECT $COLUMN_TOKEN FROM $TABLE_NAME", null)
        var token: String? = null
        if (cursor?.moveToFirst() == true) {
            token = cursor.getString(0)
        }
        cursor?.close()
        return token
    }
}
