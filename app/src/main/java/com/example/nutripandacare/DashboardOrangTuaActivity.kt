package com.example.nutripandacare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nutripandacare.databinding.ActivityDashboardOrangTuaBinding


class DashboardOrangTuaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardOrangTuaBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardOrangTuaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Hubungkan Bottom Nav dengan Navigation
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}