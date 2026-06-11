package com.example.nutripandacare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nutripandacare.databinding.ActivityDashboardGuruBinding

class DashboardGuruActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardGuruBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardGuruBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Hubungkan bottom nav dengan navController
        // Item ID di bottom_nav_guru.xml harus sama dengan ID fragment di nav_guru.xml
        binding.bottomNavigation.setupWithNavController(navController)

        // Sembunyikan bottom nav di fragment yang bukan top-level
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevelIds = setOf(
                R.id.nav_home,
                R.id.nav_rekap_gizi,
                R.id.nav_konten_edukasi,
                R.id.nav_notifikasi_guru,
                R.id.nav_aduan
            )
            val shouldShow = destination.id in topLevelIds
            if (shouldShow) {
                binding.bottomNavigation.visibility = android.view.View.VISIBLE
            } else {
                binding.bottomNavigation.visibility = android.view.View.GONE
            }
        }
    }
}