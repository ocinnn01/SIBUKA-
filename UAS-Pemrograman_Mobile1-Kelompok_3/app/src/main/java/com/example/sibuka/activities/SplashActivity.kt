package com.example.sibuka.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sibuka.MainActivity
import com.example.sibuka.databinding.ActivitySplashBinding
import com.example.sibuka.utils.FirebaseUtils

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 2000)
    }

    private fun checkAuthenticationStatus() {
        try {
            Log.d("SplashActivity", "Checking Firebase authentication...")
            
            val currentUser = FirebaseUtils.auth.currentUser
            Log.d("SplashActivity", "Current user: ${currentUser?.uid}")

            if (currentUser != null) {
                Log.d("SplashActivity", "User logged in, going to MainActivity")
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Log.d("SplashActivity", "User not logged in, going to LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error checking authentication: ${e.message}")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
