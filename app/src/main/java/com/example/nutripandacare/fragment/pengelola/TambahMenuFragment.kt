package com.example.nutripandacare.fragment.pengelola

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nutripandacare.R
import com.example.nutripandacare.supabase.SupabaseHelper
import com.example.nutripandacare.databinding.FragmentTambahMenuBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TambahMenuFragment : Fragment() {

    private var _binding: FragmentTambahMenuBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri?   = null
    private var cameraImageUri: Uri? = null
    private var existingImageUrl = ""
    private val calendar         = Calendar.getInstance()

    private var tanggalEdit: String? = null
    private val isEditMode get() = tanggalEdit != null

    // Launcher Galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "menu_foto")
                imageUri = cached ?: it
                updateImagePreview(imageUri)
            }
        }

    // Launcher Kamera
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                imageUri = cameraImageUri
                updateImagePreview(imageUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("camera_uri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("camera_uri")
            }
            imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("image_uri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("image_uri")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("camera_uri", cameraImageUri)
        outState.putParcelable("image_uri", imageUri)
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
            binding.tvTitle.text        = "Edit Menu MBG"
            binding.btnSimpan.text      = "Simpan Perubahan"
            binding.etTanggal.isEnabled = false
            loadExistingMenu(tanggalEdit!!)
        } else {
            binding.tvTitle.text   = "Tambah Menu MBG"
            binding.btnSimpan.text = "Simpan Menu"
            imageUri?.let { updateImagePreview(it) }
        }
    }

    private fun updateImagePreview(source: Any?) {
        if (_binding == null) return

        if (source == null || (source is String && source.isEmpty())) {
            binding.llPlaceholder.visibility = View.VISIBLE
            binding.ivPreviewMedia.visibility = View.GONE
            return
        }

        binding.llPlaceholder.visibility = View.GONE
        binding.ivPreviewMedia.visibility = View.VISIBLE
        binding.ivPreviewMedia.bringToFront()

        Glide.with(this)
            .load(source)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_food_plate)
            .error(R.drawable.ic_photo_camera)
            .into(binding.ivPreviewMedia)
    }

    private fun loadExistingMenu(tanggal: String) {
        binding.btnSimpan.isEnabled = false
        FirebaseHelper.getMenuHariIni(tanggal,
            onSuccess = { data ->
                if (_binding == null || data == null) return@getMenuHariIni
                binding.etTanggal.setText(tanggal)
                binding.etNamaMenu.setText(data["nama_menu"] as? String ?: "")
                binding.etCalories.setText((data["kalori"]  as? Number)?.toInt()?.toString() ?: "")
                binding.etProtein.setText((data["protein"]  as? Number)?.toInt()?.toString() ?: "")
                binding.etFat.setText((data["lemak"]        as? Number)?.toInt()?.toString() ?: "")
                binding.etCarbs.setText((data["karbo"]      as? Number)?.toInt()?.toString() ?: "")

                existingImageUrl = data["foto_menu"] as? String ?: ""
                if (existingImageUrl.isNotEmpty() && imageUri == null) {
                    updateImagePreview(existingImageUrl)
                }

                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(tanggal)?.let { calendar.time = it }
                } catch (_: Exception) {}
                binding.btnSimpan.isEnabled = true
            },
            onError = {
                binding.btnSimpan.isEnabled = true
                Toast.makeText(requireContext(), "Gagal: $it", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            binding.etTanggal.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
        }
        binding.etTanggal.setOnClickListener {
            if (!isEditMode) {
                DatePickerDialog(requireContext(), listener,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.flUploadMedia.setOnClickListener { showImageSourceDialog() }
        binding.btnSimpan.setOnClickListener { validateAndSave() }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")) { _, which ->
                if (which == 0) openCamera() else pickImageLauncher.launch("image/*")
            }.show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "menu_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun validateAndSave() {
        val nama = binding.etNamaMenu.text.toString().trim()
        val tgl  = binding.etTanggal.text.toString().trim()
        if (nama.isEmpty() || tgl.isEmpty()) {
            Toast.makeText(requireContext(), "Nama dan Tanggal wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        if (imageUri != null) {
            SupabaseHelper.uploadImage(
                uri       = imageUri!!,
                context   = requireContext(),
                folder    = "menu_mbg",
                onSuccess = { url -> saveToFirestore(nama, tgl, url) },
                onError = {
                    setLoading(false)
                    AlertDialog.Builder(requireContext())
                        .setTitle("Upload Gagal")
                        .setMessage("Gagal upload foto. Simpan tanpa foto?")
                        .setPositiveButton("Ya") { _, _ ->
                            setLoading(true)
                            saveToFirestore(nama, tgl, existingImageUrl)
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }
            )
        } else {
            saveToFirestore(nama, tgl, existingImageUrl)
        }
    }

    private fun saveToFirestore(nama: String, tgl: String, url: String) {
        val cal = binding.etCalories.text.toString().toIntOrNull() ?: 0
        val pro = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val fat = binding.etFat.text.toString().toIntOrNull() ?: 0
        val car = binding.etCarbs.text.toString().toIntOrNull() ?: 0
        val label = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(calendar.time)

        FirebaseHelper.simpanMenuMbg(tgl, label, nama, "Menu NutriPanda", cal, pro, car, fat, 0, 100, url,
            onSuccess = {
                Toast.makeText(requireContext(), "Menu berhasil disimpan!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { setLoading(false); Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSimpan.isEnabled = !loading
        binding.btnSimpan.text = if (loading) "Menyimpan..." else if (isEditMode) "Simpan Perubahan" else "Simpan Menu"
    }

    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): Uri? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream.copyTo(it) }
            Uri.fromFile(file)
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}