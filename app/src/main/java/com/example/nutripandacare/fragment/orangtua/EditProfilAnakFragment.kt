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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        loadDataAnak()
        setupAction()
    }

    private fun loadDataAnak() {
        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId = id
                binding.etNamaAnak.setText(data["nama_anak"] as? String ?: "")
                binding.etUsiaAnak.setText(data["usia_anak"] as? String ?: "")
                binding.etUsiaBulan.setText((data["usia_bulan"] as? Long)?.toString() ?: "")
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
                    Glide.with(this).load(foto).into(binding.ivFotoAnak)
                }
            },
            onError = {
                Toast.makeText(requireContext(), "Gagal memuat data: $it", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupAction() {
        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNamaAnak.text.toString().trim()
            val usiaStr = binding.etUsiaAnak.text.toString().trim()
            val usiaBulan = binding.etUsiaBulan.text.toString().toIntOrNull() ?: 0
            val sekolah = binding.etSekolahAnak.text.toString().trim()
            val kelas = binding.etKelasAnak.text.toString().trim()
            val jk = if (binding.rbLakiLaki.isChecked) "Laki-laki" else "Perempuan"

            if (nama.isEmpty() || usiaStr.isEmpty() || sekolah.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua data utama", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>(
                "nama_anak" to nama,
                "usia_anak" to usiaStr,
                "usia_bulan" to usiaBulan,
                "sekolah_anak" to sekolah,
                "kelas" to kelas,
                "jenis_kelamin" to jk
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

        binding.btnPilihFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun saveToFirestore(updates: Map<String, Any>) {
        FirebaseHelper.db.collection("users").document(FirebaseHelper.uid)
            .collection("anak").document(anakId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profil anak berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
