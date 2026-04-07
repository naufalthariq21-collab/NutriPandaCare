package com.example.pabnilai

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sembunyikan tulisan "pabnilai" di atas
        supportActionBar?.hide()

        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.textSize = 14f
        textView.typeface = android.graphics.Typeface.MONOSPACE
        textView.setPadding(32, 32, 32, 32)

        textView.text = studiKasus2Penilaian()
        scrollView.addView(textView)
        setContentView(scrollView)
    }

    fun studiKasus2Penilaian(): String {

        // LANGKAH 1: Simpan data 3 siswa
        val namaSiswa  = arrayOf("Budi", "Sari", "Doni")
        val nilaiTugas = arrayOf(80, 60, 40)
        val nilaiUTS   = arrayOf(75, 50, 35)
        val nilaiUAS   = arrayOf(90, 55, 30)

        // LANGKAH 2: Siapkan teks output
        var hasil = "===== REKAP NILAI SISWA =====\n"

        // LANGKAH 3: Loop untuk proses setiap siswa
        for (i in namaSiswa.indices) {

            // LANGKAH 4: Ambil data siswa ke-i
            val nama  = namaSiswa[i]
            val tugas = nilaiTugas[i]
            val uts   = nilaiUTS[i]
            val uas   = nilaiUAS[i]

            // LANGKAH 5: Hitung Nilai Akhir
            // Rumus: (Tugas x 30%) + (UTS x 30%) + (UAS x 40%)
            val nilaiAkhir = (tugas * 0.30) + (uts * 0.30) + (uas * 0.40)

            // LANGKAH 6: Tentukan Grade dengan when
            val grade = when {
                nilaiAkhir >= 85 -> "A"
                nilaiAkhir >= 70 -> "B"
                nilaiAkhir >= 55 -> "C"
                nilaiAkhir >= 40 -> "D"
                else             -> "E"
            }

            // LANGKAH 7: Cek lulus atau tidak (nilai akhir >= 55)
            val statusLulus = if (nilaiAkhir >= 55) "Lulus" else "Tidak Lulus"

            // LANGKAH 8: Tambahkan hasil tiap siswa ke output
            hasil += "${i + 1}. $nama\n"
            hasil += "   Tugas:$tugas UTS:$uts UAS:$uas\n"
            hasil += "   Nilai Akhir : $nilaiAkhir\n"
            hasil += "   Grade       : $grade\n"
            hasil += "   Status      : $statusLulus\n"
            hasil += "-----------------------------\n"
        }

        hasil += "============================="

        return hasil
    }
}