package com.example.nutripandacare.fragment.guru

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.supabase.SupabaseHelper
import com.example.nutripandacare.databinding.FragmentAduanMbgBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class AduanMbgFragment : Fragment() {

    private var _binding: FragmentAduanMbgBinding? = null
    private val binding get() = _binding!!

    private var userNama: String = ""
    private var isLoading = false
    private var imageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                imageUri = cameraImageUri
                tampilkanPreviewGambar(imageUri)
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "aduan_foto")
                imageUri = cached ?: it
                tampilkanPreviewGambar(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAduanMbgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSpinner()
        loadUserData()
        setupClickListeners()
        loadRiwayatAduan()
    }

    // [FIX] Reload riwayat setiap kali fragment kembali ke foreground
    override fun onResume() {
        super.onResume()
        loadRiwayatAduan()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinner() {
        val kategori = arrayOf("Makanan", "Pelayanan", "Kebersihan", "Lainnya")
        val adapter  = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategori)
        binding.spinnerKategoriAduan.adapter = adapter
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return
        FirebaseHelper.getDataUser(uid,
            onSuccess = { data -> userNama = data["nama"] as? String ?: "Guru" },
            onError   = { }
        )
    }

    private fun tampilkanPreviewGambar(uri: Any?) {
        if (_binding == null) return
        if (uri == null) {
            binding.ivAduan.visibility           = View.GONE
            binding.layoutPlaceholder.visibility = View.VISIBLE
            return
        }
        binding.ivAduan.visibility           = View.VISIBLE
        binding.layoutPlaceholder.visibility = View.GONE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .placeholder(R.color.cream_medium)
            .into(binding.ivAduan)
    }

    private fun setupClickListeners() {
        binding.cardPilihFoto.setOnClickListener { showImageSourceDialog() }

        binding.btnKirimAduan.setOnClickListener {
            val judul    = binding.etJudulAduan.text.toString().trim()
            val isi      = binding.etIsiAduan.text.toString().trim()
            val kategori = binding.spinnerKategoriAduan.selectedItem.toString()

            if (judul.isEmpty()) {
                binding.etJudulAduan.error = "Judul tidak boleh kosong"
                return@setOnClickListener
            }
            if (isi.isEmpty()) {
                binding.etIsiAduan.error = "Isi aduan tidak boleh kosong"
                return@setOnClickListener
            }
            prepareKirimAduan(judul, isi, kategori)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto Bukti")
            .setItems(options) { _, which ->
                if (which == 0) openCamera() else pickImageLauncher.launch("image/*")
            }.show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "aduan_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun prepareKirimAduan(judul: String, isi: String, kategori: String) {
        if (isLoading) return
        isLoading = true
        setButtonLoading(true)

        if (imageUri != null) {
            SupabaseHelper.uploadImage(
                uri       = imageUri!!,
                context   = requireContext(),
                folder    = "aduan",
                onSuccess = { url -> executeKirimAduan(judul, isi, kategori, url) },
                onError   = { err ->
                    isLoading = false
                    setButtonLoading(false)
                    Toast.makeText(requireContext(), "Gagal upload foto: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            executeKirimAduan(judul, isi, kategori, "")
        }
    }

    private fun executeKirimAduan(judul: String, isi: String, kategori: String, fotoUrl: String) {
        FirebaseHelper.kirimAduan(
            judul        = judul,
            isi          = isi,
            kategori     = kategori,
            pengirimNama = userNama.ifEmpty { "Guru" },
            pengirimRole = "guru",
            fotoAduan    = fotoUrl,
            onSuccess    = {
                isLoading = false
                if (_binding == null) return@kirimAduan
                Toast.makeText(requireContext(), "Aduan berhasil terkirim ✅", Toast.LENGTH_SHORT).show()
                // Reset form
                binding.etJudulAduan.text?.clear()
                binding.etIsiAduan.text?.clear()
                imageUri = null
                tampilkanPreviewGambar(null)
                setButtonLoading(false)

                // [FIX] Delay 800ms supaya Firestore selesai commit sebelum query
                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null) loadRiwayatAduan()
                }, 800)
            },
            onError = { err ->
                isLoading = false
                if (_binding == null) return@kirimAduan
                setButtonLoading(false)
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setButtonLoading(loading: Boolean) {
        if (_binding == null) return
        binding.btnKirimAduan.isEnabled = !loading
        binding.btnKirimAduan.text      = if (loading) "Mengirim..." else "Kirim Aduan Sekarang"
    }

    private fun loadRiwayatAduan() {
        if (_binding == null) return
        // [FIX] Tampilkan loading saat fetch riwayat
        binding.tvRiwayatEmpty.visibility = View.GONE

        FirebaseHelper.getAduanSaya(
            onSuccess = { list ->
                if (_binding == null) return@getAduanSaya
                setupRiwayatRecyclerView(list)
                binding.tvRiwayatEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            // [FIX] onError sebelumnya kosong {} — sekarang tampilkan pesan error
            onError = { err ->
                if (_binding == null) return@getAduanSaya
                android.util.Log.e("AduanMbg", "Gagal muat riwayat: $err")
                Toast.makeText(requireContext(), "Gagal muat riwayat: $err", Toast.LENGTH_SHORT).show()
                binding.tvRiwayatEmpty.visibility = View.VISIBLE
            }
        )
    }

    private fun setupRiwayatRecyclerView(list: List<Pair<String, Map<String, Any?>>>) {
        binding.rvRiwayatAduan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayatAduan.adapter       = RiwayatAduanAdapter(list)
    }

    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): Uri? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val file   = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream.copyTo(it) }
            Uri.fromFile(file)
        } catch (e: Exception) { null }
    }

    inner class RiwayatAduanAdapter(
        private val data: List<Pair<String, Map<String, Any?>>>
    ) : RecyclerView.Adapter<RiwayatAduanAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvJudul: TextView    = v.findViewById(R.id.tvRiwayatJudul)
            val tvKategori: TextView = v.findViewById(R.id.tvRiwayatKategori)
            val tvStatus: TextView   = v.findViewById(R.id.tvRiwayatStatus)
            val tvBalasan: TextView  = v.findViewById(R.id.tvRiwayatBalasan)
            val tvTanggal: TextView  = v.findViewById(R.id.tvRiwayatTanggal)
            val ivFoto: ImageView    = v.findViewById(R.id.ivRiwayatFoto)
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_riwayat_aduan, p, false))

        override fun getItemCount() = data.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val (_, item) = data[pos]
            h.tvJudul.text    = item["judul_aduan"]    as? String ?: "-"
            h.tvKategori.text = item["kategori_aduan"] as? String ?: "-"

            val status = item["status_aduan"] as? String ?: "menunggu"
            h.tvStatus.text = if (status == "selesai") "✅ Selesai" else "⏳ Menunggu"
            h.tvStatus.setTextColor(
                requireContext().getColor(
                    if (status == "selesai") R.color.green_primary else R.color.pending_text
                )
            )

            val balasan = item["balasan"] as? String ?: ""
            h.tvBalasan.visibility = if (balasan.isNotEmpty()) View.VISIBLE else View.GONE
            h.tvBalasan.text       = "Balasan: $balasan"

            val ts = item["created_at"] as? Timestamp
            h.tvTanggal.text = ts?.toDate()?.let { sdf.format(it) } ?: "-"

            val foto = item["foto_aduan"] as? String ?: ""
            if (foto.isNotEmpty()) {
                h.ivFoto.visibility = View.VISIBLE
                Glide.with(h.itemView.context)
                    .load(foto)
                    .centerCrop()
                    .placeholder(R.color.cream_medium)
                    .error(R.drawable.ic_photo_camera)
                    .into(h.ivFoto)
            } else {
                h.ivFoto.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
