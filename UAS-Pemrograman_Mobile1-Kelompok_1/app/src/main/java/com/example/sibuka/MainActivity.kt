package com.example.sibuka

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.sibuka.activities.LoginActivity
import com.example.sibuka.databinding.ActivityMainBinding
import com.example.sibuka.fragments.BukuFragment
import com.example.sibuka.fragments.NotifikasiFragment
import com.example.sibuka.fragments.PeminjamFragment
import com.example.sibuka.fragments.PinjamFragment
import com.example.sibuka.fragments.ProfilFragment
import com.example.sibuka.utils.FirebaseUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPreferences
    private var switchDarkMode: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ===== Tambahan Dark Mode =====
        sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        // Set mode awal sesuai preferensi terakhir
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Temukan switchDarkMode di layout
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchDarkMode?.isChecked = isDarkMode

        // Listener untuk ubah mode
        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                saveMode(true)
                Toast.makeText(this, "🌙 Mode Gelap Aktif", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                saveMode(false)
                Toast.makeText(this, "☀️ Mode Terang Aktif", Toast.LENGTH_SHORT).show()
            }
        }
        // ===== Akhir Tambahan =====

        if (FirebaseUtils.auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        if (savedInstanceState == null) {
            loadFragment(BukuFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_buku -> {
                    loadFragment(BukuFragment())
                    true
                }
                R.id.nav_peminjam -> {
                    loadFragment(PeminjamFragment())
                    true
                }
                R.id.nav_pinjam -> {
                    loadFragment(PinjamFragment())
                    true
                }
                R.id.nav_notifikasi -> {
                    loadFragment(NotifikasiFragment())
                    true
                }
                R.id.nav_profil -> {
                    loadFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun saveMode(isDarkMode: Boolean) {
        val editor = sharedPref.edit()
        editor.putBoolean("dark_mode", isDarkMode)
        editor.apply()
    }
}
