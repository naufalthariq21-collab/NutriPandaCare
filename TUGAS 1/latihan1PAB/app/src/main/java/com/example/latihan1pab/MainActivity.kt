package com.example.latihan1pab

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Deklarasi variable counter, nilai awal = 0
    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Hubungkan variable ke View yang ada di XML
        val txtView = findViewById<TextView>(R.id.txt_view)
        val btnCount = findViewById<Button>(R.id.btn_count)
        val btnReset = findViewById<Button>(R.id.btn_reset)

        // Logika tombol COUNT → tambah counter lalu update tampilan
        btnCount.setOnClickListener {
            counter+1
            txtView.text = counter.toString()
        }

        // Logika tombol RESET → kembalikan counter ke 0 lalu update tampilan
        btnReset.setOnClickListener {
            counter = 0
            txtView.text = getString(R.string.result_counters)
        }
    }
}