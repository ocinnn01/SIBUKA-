package com.example.sibuka.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sibuka.MainActivity
import com.example.sibuka.databinding.ActivityLoginBinding
import com.example.sibuka.utils.FirebaseUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupClickListeners()

            // Sembunyikan progress bar di awal
            binding.progressBar.visibility = View.GONE

            Log.d(TAG, "LoginActivity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Terjadi kesalahan saat membuka halaman login", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnLogin.setOnClickListener {
                loginUser() // fungsi login dijalankan di sini
            }

            binding.tvRegister.setOnClickListener {
                navigateToRegister()
            }

            Log.d(TAG, "Click listeners setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat tombol", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Mohon isi email dan password", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan progress bar dan ubah tombol login
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Sedang login..."

        FirebaseUtils.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"

                if (task.isSuccessful) {
                    val username = email.substringBefore("@")
                    Toast.makeText(this, "Selamat datang, $username!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Login gagal"
                    Log.e(TAG, "Login gagal: $errorMessage")
                    Toast.makeText(this, "Login gagal: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToRegister() {
        try {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka halaman register", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoginActivity destroyed")
    }
}
