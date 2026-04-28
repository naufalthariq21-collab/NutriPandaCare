package com.example.nutripandacare.fragment.pengelola

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.nutripandacare.databinding.FragmentTambahMenuBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TambahMenuFragment : Fragment() {

    private var _binding: FragmentTambahMenuBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private val calendar = Calendar.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.ivPreviewMedia.setImageURI(it)
            binding.ivPreviewMedia.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePicker()
        setupClickListeners()
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateLabel()
        }

        binding.etTanggal.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.etTanggal.setText(sdf.format(calendar.time))
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.flUploadMedia.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSimpan.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val nama = binding.etNamaMenu.text.toString().trim()
        val tanggal = binding.etTanggal.text.toString().trim()
        val kalori = binding.etCalories.text.toString().toIntOrNull() ?: 0
        val protein = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val lemak = binding.etFat.text.toString().toIntOrNull() ?: 0
        val karbo = binding.etCarbs.text.toString().toIntOrNull() ?: 0

        if (nama.isEmpty() || tanggal.isEmpty()) {
            Toast.makeText(requireContext(), "Nama dan Tanggal wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Menyimpan..."

        if (imageUri != null) {
            uploadImage(imageUri!!) { url ->
                saveToFirestore(nama, tanggal, kalori, protein, lemak, karbo, url ?: "")
            }
        } else {
            saveToFirestore(nama, tanggal, kalori, protein, lemak, karbo, "")
        }
    }

    private fun uploadImage(uri: Uri, onComplete: (String?) -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference.child("menu_mbg/$fileName.jpg")

        ref.putFile(uri)
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { url -> onComplete(url.toString()) }
            .addOnFailureListener { onComplete(null) }
    }

    private fun saveToFirestore(
        nama: String, tanggal: String, kalori: Int, protein: Int,
        lemak: Int, karbo: Int, imageUrl: String
    ) {
        val labelSdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val tanggalLabel = labelSdf.format(calendar.time)

        FirebaseHelper.simpanMenuMbg(
            tanggal = tanggal,
            tanggalLabel = tanggalLabel,
            namaMenu = nama,
            deskripsi = "Menu sehat harian NutriPanda",
            kalori = kalori,
            protein = protein,
            karbo = karbo,
            lemak = lemak,
            serat = 0,
            persentaseNutrisi = 100,
            fotoMenu = imageUrl,
            onSuccess = {
                if (_binding == null) return@simpanMenuMbg
                Toast.makeText(requireContext(), "Menu berhasil disimpan!", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onError = { err ->
                if (_binding == null) return@simpanMenuMbg
                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "Simpan Perubahan"
                Toast.makeText(requireContext(), "Error: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
