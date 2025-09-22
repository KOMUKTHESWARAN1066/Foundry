package com.MaDuSOFTSolutions.foundry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get SharedPreferences
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Optional safety: clear login if "exitApp" flag is set
        if (sharedPref.getBoolean("exitApp", false)) {
            sharedPref.edit().clear().apply()
        }

        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        Log.d("sharedpreferences", isLoggedIn.toString())

        // Bottom navigation setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val menu = bottomNav.menu

        val loginItem = menu.findItem(R.id.menu_login)
        val logoutItem = menu.findItem(R.id.menu_Logout)
        val heatItem = menu.findItem(R.id.menu_heat_status)
        val purchaseItem = menu.findItem(R.id.menu_purchase)
        val exitItem = menu.findItem(R.id.menu_exit)

        val canViewPurchase = sharedPref.getBoolean("canViewPurchase", false)



        // Set visibility based on login state
        if (isLoggedIn) {
            loginItem.isVisible = false
            heatItem.isVisible = true
            purchaseItem.isVisible = canViewPurchase
            logoutItem.isVisible = true
            exitItem.isVisible = true
        } else {
            loginItem.isVisible = true
            heatItem.isVisible = false
            purchaseItem.isVisible = false
            logoutItem.isVisible = false
            exitItem.isVisible = true
        }



        // Handle bottom menu actions
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }

                R.id.menu_heat_status -> {
                    startActivity(Intent(this, HeatStatusActivity::class.java))
                    true
                }

                R.id.menu_purchase -> {
                    startActivity(Intent(this, PurchaseActivity::class.java))
                    true
                }
                // In the exit menu case:
                R.id.menu_exit -> {
                    // Stop the service first
                    stopService(Intent(this, LogoutService::class.java)) // <-- ADD THIS

                    // Clear SharedPreferences
                    sharedPref.edit()
                        .putBoolean("exitApp", true)
                        .putBoolean("isLoggedIn", false)
                        .commit()

                    finishAffinity()
                    System.exit(0)
                    true
                }

                R.id.menu_Logout -> {
                    stopService(Intent(this, LogoutService::class.java))
                    sharedPref.edit().clear().apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    true  // <-- Add this line
                }



                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean = false
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = false
}
