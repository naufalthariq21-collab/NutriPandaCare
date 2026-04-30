package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentMenuMbgBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class MenuMbgFragment : Fragment() {

    private var _binding: FragmentMenuMbgBinding? = null
    private val binding get() = _binding!!

    private var calendar = Calendar.getInstance()
    private var selectedDate = Date()
    private var menuDataMap = mutableMapOf<String, Map<String, Any?>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuMbgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendarHeader()
        fetchMenuData()
        
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCalendarHeader() {
        updateMonthYearDisplay()

        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthYearDisplay()
            fetchMenuData()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthYearDisplay()
            fetchMenuData()
        }
    }

    private fun updateMonthYearDisplay() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        binding.tvMonthYear.text = sdf.format(calendar.time)
    }

    private fun fetchMenuData() {
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        // Load some margin to show dots on adjacent month days too if visible
        tempCal.add(Calendar.DAY_OF_MONTH, -7)
        val start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)
        
        tempCal.add(Calendar.DAY_OF_MONTH, 45) // cover the whole grid
        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)

        FirebaseHelper.getDaftarMenu(start, end,
            onSuccess = { list ->
                if (_binding == null) return@getDaftarMenu
                menuDataMap.clear()
                list.forEach { menu ->
                    val tgl = menu["tanggal"] as? String ?: ""
                    menuDataMap[tgl] = menu
                }
                renderCalendar()
                updateMenuDetail(selectedDate)
            },
            onError = { err ->
                if (_binding == null) return@getDaftarMenu
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                renderCalendar()
            }
        )
    }

    private fun renderCalendar() {
        val days = mutableListOf<Date>()
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        // Find the first day of the week (Sunday is 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        
        // Roll back to the start of the week
        cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
        
        // Fill exactly 42 days (6 rows of 7 days)
        for (i in 0 until 42) {
            days.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val adapter = CalendarAdapter(
            days, 
            selectedDate, 
            menuDataMap.keys, 
            calendar.get(Calendar.MONTH)
        ) { date ->
            selectedDate = date
            renderCalendar()
            updateMenuDetail(date)
        }
        
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = adapter
    }

    private fun updateMenuDetail(date: Date) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateKey = sdf.format(date)
        val labelSdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        
        binding.tvSelectedDateLabel.text = "Menu ${labelSdf.format(date)}"

        val menu = menuDataMap[dateKey]
        if (menu != null) {
            binding.cardDetailMenu.visibility = View.VISIBLE
            binding.layoutEmptyMenu.visibility = View.GONE

            binding.tvNamaMenuDetail.text = menu["nama_menu"] as? String
            binding.tvDescMenuDetail.text = menu["deskripsi"] as? String ?: "Menu sehat NutriPanda"
            binding.tvValKalori.text = menu["kalori"]?.toString() ?: "0"
            binding.tvValProtein.text = "${menu["protein"]}g"
            binding.tvValKarbo.text = "${menu["karbo"]}g"
            binding.tvValLemak.text = "${menu["lemak"]}g"

            val fotoUrl = menu["foto_menu"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                Glide.with(this).load(fotoUrl).placeholder(R.drawable.ic_food_plate).into(binding.ivMenuDetail)
            } else {
                binding.ivMenuDetail.setImageResource(R.drawable.ic_food_plate)
            }
        } else {
            binding.cardDetailMenu.visibility = View.GONE
            binding.layoutEmptyMenu.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
