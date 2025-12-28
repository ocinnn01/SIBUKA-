package com.example.sibuka.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sibuka.MainActivity
import com.example.sibuka.databinding.ActivityRegisterBinding
import com.example.sibuka.models.AdminUser
import com.example.sibuka.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityRegisterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            initializeFirebase()

            setupClickListeners()

            Log.d(TAG, "RegisterActivity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error loading register page: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeFirebase() {
        try {
            auth = FirebaseUtils.auth
            firestore = FirebaseUtils.firestore
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            throw e
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnRegister.setOnClickListener {
                registerAdmin()
            }

            binding.tvLogin.setOnClickListener {
                onBackPressed()
            }

            Log.d(TAG, "Click listeners setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
            throw e
        }
    }

    private fun registerAdmin() {
        try {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (!validateInput(name, email, password, confirmPassword)) {
                return
            }
            setLoadingState(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            saveAdminToFirestore(it.uid, name, email)
                        } ?: run {
                            setLoadingState(false)
                            Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        setLoadingState(false)
                        val errorMessage = task.exception?.message ?: "Registration failed"
                        Log.e(TAG, "Registration failed: $errorMessage")
                        Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in registerAdmin: ${e.message}", e)
            setLoadingState(false)
            Toast.makeText(this, "Error during registration", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        binding.etName.error = null
        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null

        var isValid = true

        if (name.isEmpty()) {
            binding.etName.error = "Nama tidak boleh kosong"
            binding.etName.requestFocus()
            isValid = false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            if (isValid) binding.etEmail.requestFocus()
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Format email tidak valid"
            if (isValid) binding.etEmail.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password tidak boleh kosong"
            if (isValid) binding.etPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error = "Password minimal 6 karakter"
            if (isValid) binding.etPassword.requestFocus()
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Konfirmasi password tidak boleh kosong"
            if (isValid) binding.etConfirmPassword.requestFocus()
            isValid = false
        } else if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Password tidak sama"
            if (isValid) binding.etConfirmPassword.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun saveAdminToFirestore(uid: String, name: String, email: String) {
        try {
            val adminUser = AdminUser(
                id = uid,
                name = name,
                email = email,
                role = "admin",
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(FirebaseUtils.COLLECTION_ADMIN_USERS)
                .document(uid)
                .set(adminUser)
                .addOnSuccessListener {
                    setLoadingState(false)
                    Log.d(TAG, "Admin saved to Firestore successfully")
                    Toast.makeText(this, "Registrasi berhasil! Silakan login.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    finish()
                }
                .addOnFailureListener { exception ->
                    setLoadingState(false)
                    Log.e(TAG, "Error saving admin to Firestore: ${exception.message}", exception)
                    Toast.makeText(this, "Error saving user data: ${exception.message}", Toast.LENGTH_LONG).show()
                    auth.currentUser?.delete()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveAdminToFirestore: ${e.message}", e)
            setLoadingState(false)
            Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        try {
            binding.btnRegister.isEnabled = !isLoading
            binding.btnRegister.text = if (isLoading) "Mendaftar..." else "Daftar"

            binding.etName.isEnabled = !isLoading
            binding.etEmail.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
            binding.etConfirmPassword.isEnabled = !isLoading
            binding.tvLogin.isEnabled = !isLoading
        } catch (e: Exception) {
            Log.e(TAG, "Error setting loading state: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RegisterActivity destroyed")
    }
}
