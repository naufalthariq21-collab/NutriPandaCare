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
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.databinding.FragmentTambahMenuBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TambahMenuFragment : Fragment() {

    private var _binding: FragmentTambahMenuBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri?   = null
    private var existingImageUrl = "" // URL foto yang sudah ada (mode edit)
    private val calendar         = Calendar.getInstance()

    // Apakah fragment ini di mode edit (dapat arg tanggal_edit)
    private var tanggalEdit: String? = null
    private val isEditMode get() = tanggalEdit != null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

        tanggalEdit = arguments?.getString("tanggal_edit")

        setupDatePicker()
        setupClickListeners()

        if (isEditMode) {
            binding.tvTitle.text      = "Edit Menu MBG"
            binding.btnSimpan.text    = "Simpan Perubahan"
            binding.etTanggal.isEnabled = false  // tanggal tidak bisa diubah di mode edit
            loadExistingMenu(tanggalEdit!!)
        } else {
            binding.tvTitle.text   = "Tambah Menu MBG"
            binding.btnSimpan.text = "Simpan Menu"
        }
    }

    // ─── LOAD DATA (mode edit) ────────────────────────────────────────────────

    private fun loadExistingMenu(tanggal: String) {
        binding.btnSimpan.isEnabled = false
        FirebaseHelper.getMenuHariIni(tanggal,
            onSuccess = { data ->
                if (_binding == null || data == null) return@getMenuHariIni
                binding.etTanggal.setText(tanggal)
                binding.etNamaMenu.setText(data["nama_menu"] as? String ?: "")
                binding.etCalories.setText((data["kalori"] as? Number)?.toInt()?.toString() ?: "")
                binding.etProtein.setText((data["protein"] as? Number)?.toInt()?.toString() ?: "")
                binding.etFat.setText((data["lemak"] as? Number)?.toInt()?.toString() ?: "")
                binding.etCarbs.setText((data["karbo"] as? Number)?.toInt()?.toString() ?: "")

                existingImageUrl = data["foto_menu"] as? String ?: ""
                if (existingImageUrl.isNotEmpty()) {
                    binding.ivPreviewMedia.visibility = View.VISIBLE
                    Glide.with(this).load(existingImageUrl)
                        .placeholder(com.example.nutripandacare.R.drawable.ic_food_plate)
                        .into(binding.ivPreviewMedia)
                }

                // Set calendar agar label tanggal benar
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(tanggal)?.let { calendar.time = it }
                } catch (_: Exception) {}

                binding.btnSimpan.isEnabled = true
            },
            onError = {
                binding.btnSimpan.isEnabled = true
                Toast.makeText(requireContext(), "Gagal memuat menu: $it", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── DATE PICKER ──────────────────────────────────────────────────────────

    private fun setupDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateDateLabel()
        }
        binding.etTanggal.setOnClickListener {
            if (!isEditMode) {
                DatePickerDialog(
                    requireContext(), listener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        binding.etTanggal.setText(sdf.format(calendar.time))
    }

    // ─── CLICK LISTENERS ──────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.flUploadMedia.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnSimpan.setOnClickListener {
            validateAndSave()
        }
    }

    // ─── VALIDASI & SIMPAN ────────────────────────────────────────────────────

    private fun validateAndSave() {
        val nama    = binding.etNamaMenu.text.toString().trim()
        val tanggal = binding.etTanggal.text.toString().trim()
        val kalori  = binding.etCalories.text.toString().toIntOrNull() ?: 0
        val protein = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val lemak   = binding.etFat.text.toString().toIntOrNull() ?: 0
        val karbo   = binding.etCarbs.text.toString().toIntOrNull() ?: 0

        if (nama.isEmpty()) {
            binding.etNamaMenu.error = "Nama menu wajib diisi"
            return
        }
        if (tanggal.isEmpty()) {
            binding.etTanggal.error = "Tanggal wajib dipilih"
            return
        }

        setLoading(true)

        if (imageUri != null) {
            // Ada foto baru dipilih → upload dulu
            uploadImage(imageUri!!) { url ->
                saveToFirestore(nama, tanggal, kalori, protein, lemak, karbo, url ?: existingImageUrl)
            }
        } else {
            // Pakai URL lama (kosong kalau memang tidak ada foto)
            saveToFirestore(nama, tanggal, kalori, protein, lemak, karbo, existingImageUrl)
        }
    }

    private fun uploadImage(uri: Uri, onComplete: (String?) -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference.child("menu_mbg/$fileName.jpg")
        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { url -> onComplete(url.toString()) }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Upload foto gagal, menu disimpan tanpa foto", Toast.LENGTH_SHORT).show()
                onComplete(null)
            }
    }

    private fun saveToFirestore(
        nama: String, tanggal: String,
        kalori: Int, protein: Int, lemak: Int, karbo: Int,
        imageUrl: String
    ) {
        val labelSdf    = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val tanggalLabel = labelSdf.format(calendar.time)

        FirebaseHelper.simpanMenuMbg(
            tanggal          = tanggal,
            tanggalLabel     = tanggalLabel,
            namaMenu         = nama,
            deskripsi        = "Menu sehat harian NutriPanda",
            kalori           = kalori,
            protein          = protein,
            karbo            = karbo,
            lemak            = lemak,
            serat            = 0,
            persentaseNutrisi = 100,
            fotoMenu         = imageUrl,
            onSuccess = {
                if (_binding == null) return@simpanMenuMbg
                val msg = if (isEditMode) "Menu berhasil diperbarui!" else "Menu berhasil disimpan!"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { err ->
                if (_binding == null) return@simpanMenuMbg
                setLoading(false)
                Toast.makeText(requireContext(), "Error: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSimpan.isEnabled = !loading
        binding.btnSimpan.text      = if (loading) "Menyimpan..." else if (isEditMode) "Simpan Perubahan" else "Simpan Menu"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}