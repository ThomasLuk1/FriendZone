package com.example.friendzone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = (application as FriendZoneApp).auth

        val background = object: Thread() {
            override fun run() {
                super.run()
                var intent: Intent? = null
                try {
                    Thread.sleep(2000)
                    if (auth.currentUser != null) {
                        intent = Intent(baseContext, UserContent::class.java).apply {
                            putExtra(UserContent.USER_KEY, auth.currentUser)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        intent = Intent(baseContext, MainActivity::class.java)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        background.start()
    }
}
