package com.example.nutripandacare

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nutripandacare.databinding.ActivityDashboardPengelolaBinding

class DashboardPengelolaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardPengelolaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardPengelolaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Hubungkan bottom nav dengan navController
        // Item ID di bottom_nav_pengelola.xml harus sama dengan ID fragment di nav_pengelola.xml
        binding.bottomNavigation.setupWithNavController(navController)

        // Sembunyikan bottom nav di halaman non-top-level (misal TambahMenu)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevelIds = setOf(
                R.id.fragment_home_pengelola,
                R.id.fragment_verifikasi_pengelola,
                R.id.fragment_aduan_pengelola,
                R.id.fragment_menu_mbg,
                R.id.fragment_profil_pengelola
            )
            binding.bottomNavigation.visibility =
                if (destination.id in topLevelIds) View.VISIBLE else View.GONE
        }
    }
}