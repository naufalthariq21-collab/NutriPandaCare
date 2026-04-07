package com.example.pabkasir

import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sembunyikan action bar (tulisan "pabkasir" di atas)
        supportActionBar?.hide()

        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.textSize = 14f
        textView.typeface = android.graphics.Typeface.MONOSPACE
        textView.setPadding(32, 32, 32, 32)

        textView.text = studiKasus1Kasir()
        scrollView.addView(textView)
        setContentView(scrollView)
    }

    fun studiKasus1Kasir(): String {

        val namaPembeli = "Andi"

        val namaBarang1 = "Buku Tulis"
        val namaBarang2 = "Pensil"
        val namaBarang3 = "Penghapus"

        val harga1 = 5000
        val harga2 = 2500
        val harga3 = 1500

        val jumlah1 = 3
        val jumlah2 = 2
        val jumlah3 = 1

        val totalBarang1 = harga1 * jumlah1
        val totalBarang2 = harga2 * jumlah2
        val totalBarang3 = harga3 * jumlah3

        val subtotal = totalBarang1 + totalBarang2 + totalBarang3

        val persenDiskon: Int
        val keteranganDiskon: String

        if (subtotal > 20000) {
            persenDiskon = 10
            keteranganDiskon = "Diskon 10%"
        } else if (subtotal > 10000) {
            persenDiskon = 5
            keteranganDiskon = "Diskon 5%"
        } else {
            persenDiskon = 0
            keteranganDiskon = "Tidak Ada Diskon"
        }

        val nilaiDiskon = subtotal * persenDiskon / 100
        val totalBayar  = subtotal - nilaiDiskon

        return """
===== STRUK BELANJA =====
Pembeli : $namaPembeli
-------------------------
$namaBarang1  x$jumlah1 = Rp$totalBarang1
$namaBarang2       x$jumlah2 = Rp$totalBarang2
$namaBarang3    x$jumlah3 = Rp$totalBarang3
-------------------------
Subtotal    : Rp$subtotal
$keteranganDiskon : Rp$nilaiDiskon
Total Bayar : Rp$totalBayar
=========================
        """.trimIndent()
    }
}