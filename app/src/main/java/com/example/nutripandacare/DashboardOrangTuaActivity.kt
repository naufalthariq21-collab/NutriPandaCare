package com.example.nutripandacare

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nutripandacare.databinding.ActivityDashboardOrangTuaBinding

class DashboardOrangTuaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardOrangTuaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardOrangTuaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Hubungkan bottom nav dengan navController
        // Item ID di bottom_nav_orang_tua.xml harus sama dengan ID fragment di nav_orang_tua.xml
        binding.bottomNavigation.setupWithNavController(navController)

        // Sembunyikan bottom nav di halaman detail / sub-halaman
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevelIds = setOf(
                R.id.nav_home_ortu,
                R.id.nav_edukasi,
                R.id.nav_gizi,
                R.id.nav_menu_mbg
            )
            binding.bottomNavigation.visibility =
                if (destination.id in topLevelIds) View.VISIBLE else View.GONE
        }
    }
}