package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentRekapGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class RekapGiziFragment : Fragment() {

    private var _binding: FragmentRekapGiziBinding? = null
    private val binding get() = _binding!!

    private val semuaSiswa = mutableListOf<Map<String, Any?>>()
    private val filteredSiswa = mutableListOf<Map<String, Any?>>()
    private lateinit var adapter: SiswaAdapter

    private var filterAktif = "semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRekapGiziBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupChips()
        setupClickListeners()
        loadSemuaSiswa()
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(filteredSiswa) { siswaData ->
            // Detail click handling - placeholder for now
            Toast.makeText(requireContext(), "Detail: ${siswaData["nama_siswa"]}", Toast.LENGTH_SHORT).show()
        }
        binding.rvDaftarSiswa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDaftarSiswa.adapter = adapter
    }

    private fun loadSemuaSiswa() {
        FirebaseHelper.getRekapGizi(
            onSuccess = { rekapList ->
                if (_binding == null) return@getRekapGizi
                semuaSiswa.clear()

                if (rekapList.isEmpty()) {
                    applyFilter()
                    return@getRekapGizi
                }

                var selesai = 0
                rekapList.forEach { (rekapId, _) ->
                    FirebaseHelper.getDetailSiswa(
                        rekapId = rekapId,
                        onSuccess = { siswaList ->
                            siswaList.forEach { siswaData ->
                                semuaSiswa.add(siswaData + mapOf("rekap_id" to rekapId))
                            }
                            selesai++
                            if (selesai == rekapList.size) applyFilter()
                        },
                        onError = { _ ->
                            selesai++
                            if (selesai == rekapList.size) applyFilter()
                        }
                    )
                }
            },
            onError = { err ->
                if (_binding == null) return@getRekapGizi
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyFilter() {
        val query = binding.etCariSiswa.text.toString().trim().lowercase()

        val filtered = semuaSiswa.filter { siswa ->
            val nama = (siswa["nama_siswa"] as? String ?: "").lowercase()
            val statusGizi = (siswa["status_gizi"] as? String ?: "").lowercase()
            val namaMatch = nama.contains(query)

            val filterMatch = when (filterAktif) {
                "normal" -> statusGizi == "normal"
                "berisiko" -> statusGizi != "normal"
                else -> true
            }
            namaMatch && filterMatch
        }

        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun setupSearch() {
        binding.etCariSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })
    }

    private fun setupChips() {
        binding.chipSemua.setOnClickListener { 
            filterAktif = "semua"
            applyFilter() 
        }
        binding.chipNormal.setOnClickListener { 
            filterAktif = "normal"
            applyFilter() 
        }
        binding.chipResiko.setOnClickListener { 
            filterAktif = "berisiko"
            applyFilter() 
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SiswaAdapter(
        private val data: List<Map<String, Any?>>,
        private val onClick: (Map<String, Any?>) -> Unit
    ) : RecyclerView.Adapter<SiswaAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvInisial: TextView = view.findViewById(R.id.tvInisialSiswa)
            val tvNama: TextView = view.findViewById(R.id.tvNamaSiswa)
            val tvInfo: TextView = view.findViewById(R.id.tvInfoSiswa)
            val tvStatus: TextView = view.findViewById(R.id.tvStatusGiziSiswa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_siswa, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa = data[position]
            val nama = siswa["nama_siswa"] as? String ?: "-"
            val kelas = siswa["kelas"] as? String ?: "-"
            val zScore = (siswa["z_score"] as? Number)?.toDouble() ?: 0.0
            val statusGizi = siswa["status_gizi"] as? String ?: "Normal"

            val inisial = nama.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }
            holder.tvInisial.text = inisial.uppercase()
            holder.tvNama.text = nama
            holder.tvInfo.text = "$kelas • Z-Score: ${"%.1f".format(zScore)}"
            holder.tvStatus.text = statusGizi

            val (bgColor, txtColor) = when (statusGizi.lowercase()) {
                "normal" -> Pair(R.color.green_pastel, R.color.green_primary)
                "gizi kurang",
                "gizi buruk" -> Pair(R.color.pending_bg, R.color.pending_text)
                else -> Pair(R.color.cream_warm, R.color.text_secondary)
            }
            
            holder.tvStatus.setBackgroundResource(bgColor)
            holder.tvStatus.setTextColor(requireContext().getColor(txtColor))

            holder.itemView.setOnClickListener { onClick(siswa) }
        }
    }
}
