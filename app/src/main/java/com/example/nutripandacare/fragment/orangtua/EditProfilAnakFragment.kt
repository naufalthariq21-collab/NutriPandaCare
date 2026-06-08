package com.example.nutripandacare.fragment.orangtua

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentEditProfilAnakBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.firestore.SetOptions

class EditProfilAnakFragment : Fragment() {

    private var _binding: FragmentEditProfilAnakBinding? = null
    private val binding get() = _binding!!
    private var anakId: String = ""
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.ivFotoAnak.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfilAnakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadDataAnak()
        setupAction()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.inflateMenu(R.menu.menu_save)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save) {
                performSave()
                true
            } else {
                false
            }
        }
    }

    private fun loadDataAnak() {
        // Biarkan tombol simpan menyala agar bisa input data baru jika kosong
        binding.btnSimpan.isEnabled = true
        
        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId = id
                
                binding.etNamaAnak.setText(data["nama_anak"] as? String ?: "")
                binding.etUsiaAnak.setText(data["usia_anak"] as? String ?: "")
                binding.etUsiaBulan.setText((data["usia_bulan"] as? Number)?.toLong()?.toString() ?: "")
                binding.etSekolahAnak.setText(data["sekolah_anak"] as? String ?: "")
                binding.etKelasAnak.setText(data["kelas"] as? String ?: "")

                val jk = data["jenis_kelamin"] as? String ?: ""
                if (jk.contains("Laki", true)) {
                    binding.rbLakiLaki.isChecked = true
                } else if (jk.contains("Perempuan", true)) {
                    binding.rbPerempuan.isChecked = true
                }

                val foto = data["foto_anak"] as? String ?: ""
                if (foto.isNotEmpty()) {
                    Glide.with(this).load(foto).placeholder(R.drawable.ic_placeholder_child).into(binding.ivFotoAnak)
                }
            },
            onError = {
                if (_binding == null) return@getDataAnak
                // Jika error karena data belum ada, tidak perlu tampilkan toast error yang mengganggu
                if (!it.contains("belum ada", true)) {
                    Toast.makeText(requireContext(), "Info: $it", Toast.LENGTH_SHORT).show()
                }
                anakId = "" // Menandakan data baru
            }
        )
    }

    private fun setupAction() {
        binding.btnSimpan.setOnClickListener {
            performSave()
        }

        binding.btnPilihFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun performSave() {
        val nama = binding.etNamaAnak.text.toString().trim()
        val usiaStr = binding.etUsiaAnak.text.toString().trim()
        val usiaBulan = binding.etUsiaBulan.text.toString().toIntOrNull() ?: 0
        val sekolah = binding.etSekolahAnak.text.toString().trim()
        val kelas = binding.etKelasAnak.text.toString().trim()
        val jk = if (binding.rbLakiLaki.isChecked) "Laki-laki" else "Perempuan"

        if (nama.isEmpty() || usiaStr.isEmpty() || sekolah.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi semua data utama", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Menyimpan..."

        // Jika data baru, buat ID dokumen baru
        if (anakId.isEmpty()) {
            anakId = FirebaseHelper.db.collection("users").document(FirebaseHelper.uid)
                .collection("anak").document().id
        }

        val updates = mutableMapOf<String, Any>(
            "nama_anak" to nama,
            "usia_anak" to usiaStr,
            "usia_bulan" to usiaBulan,
            "sekolah_anak" to sekolah,
            "kelas" to kelas,
            "jenis_kelamin" to jk,
            "updated_at" to com.google.firebase.Timestamp.now()
        )

        if (imageUri != null) {
            FirebaseHelper.uploadImage("anak/$anakId.jpg", imageUri!!,
                onSuccess = { url ->
                    updates["foto_anak"] = url
                    saveToFirestore(updates)
                },
                onError = {
                    Toast.makeText(requireContext(), "Gagal upload foto: $it", Toast.LENGTH_SHORT).show()
                    saveToFirestore(updates)
                }
            )
        } else {
            saveToFirestore(updates)
        }
    }

    private fun saveToFirestore(updates: Map<String, Any>) {
        if (anakId.isEmpty()) return

        FirebaseHelper.db.collection("users").document(FirebaseHelper.uid)
            .collection("anak").document(anakId)
            .set(updates, SetOptions.merge()) // Gunakan set merge agar bisa create atau update
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Data anak berhasil disimpan", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan Perubahan"
                    Toast.makeText(requireContext(), "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
