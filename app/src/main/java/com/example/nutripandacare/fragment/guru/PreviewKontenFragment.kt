package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentPreviewKontenBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class PreviewKontenFragment : Fragment() {

    private var _binding: FragmentPreviewKontenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewKontenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            try {
                val popped = findNavController().popBackStack(R.id.nav_konten_edukasi, false)
                if (!popped) requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        val artikelId = arguments?.getString("artikel_id")
        if (!artikelId.isNullOrEmpty()) {
            loadDetailArtikel(artikelId)
        } else {
            Toast.makeText(requireContext(), "ID artikel tidak ditemukan", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadDetailArtikel(id: String) {
        FirebaseHelper.db.collection("artikel").document(id).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                if (doc.exists()) {
                    binding.tvJudul.text     = doc.getString("judul")    ?: "-"
                    binding.tvKategori.text  = doc.getString("kategori") ?: "-"
                    binding.tvIsiKonten.text = doc.getString("isi_konten") ?: "-"
                    binding.tvWaktuBaca.text = "${doc.getLong("menit_baca") ?: 0} menit baca"

                    val thumb = doc.getString("thumbnail_url") ?: ""
                    tampilkanThumbnail(thumb)
                } else {
                    Toast.makeText(requireContext(), "Artikel tidak ditemukan", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat konten: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * [FIX] Sebelumnya hanya `Glide.with(this).load(thumb).into(...)` tanpa
     * placeholder, tanpa error handler, dan tanpa cache strategy — kalau
     * gagal load (delay propagasi CDN Firebase Storage setelah upload baru,
     * URL belum fresh, dsb) gambar diam-diam kosong tanpa indikasi apapun.
     *
     * Sekarang ditambahkan:
     * - placeholder & error drawable supaya selalu ada visual feedback
     * - DiskCacheStrategy.NONE + skipMemoryCache supaya tidak menyimpan
     *   hasil gagal/placeholder secara permanen di cache
     * - RequestListener untuk log error asli ke Logcat (membantu debug)
     */
    private fun tampilkanThumbnail(url: String) {
        if (_binding == null) return
        if (url.isEmpty()) {
            binding.ivThumbnail.setImageResource(R.drawable.ic_food_plate)
            return
        }
        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_food_plate)
            .error(R.drawable.ic_photo_camera)
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("PreviewKonten", "Gagal load thumbnail: $url", e)
                    return false
                }
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(binding.ivThumbnail)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
