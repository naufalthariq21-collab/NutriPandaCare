package com.example.nutripandacare.fragment.orangtua

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
        setupSwipeGesture()
        fetchMenuData()
        
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCalendarHeader() {
        updateMonthYearDisplay()

        binding.btnPrevMonth.setOnClickListener {
            changeMonth(-1)
        }

        binding.btnNextMonth.setOnClickListener {
            changeMonth(1)
        }
    }

    private fun changeMonth(offset: Int) {
        calendar.add(Calendar.MONTH, offset)
        updateMonthYearDisplay()
        fetchMenuData()
        
        // Animasi saat ganti bulan
        val animId = if (offset > 0) R.anim.slide_in_right else R.anim.slide_in_left
        try {
            val animation = AnimationUtils.loadAnimation(requireContext(), animId)
            binding.rvCalendar.startAnimation(animation)
        } catch (e: Exception) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (Math.abs(deltaX) > 100 && Math.abs(velocityX) > 100) {
                    if (deltaX > 0) {
                        changeMonth(-1) // Swipe kanan -> bulan sebelumnya
                    } else {
                        changeMonth(1)  // Swipe kiri -> bulan berikutnya
                    }
                    return true
                }
                return false
            }
        })

        // Terapkan touch listener pada area kalender
        binding.rvCalendar.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // Biarkan RecyclerView tetap menangani klik item
            false 
        }
    }

    private fun updateMonthYearDisplay() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        binding.tvMonthYear.text = sdf.format(calendar.time)
    }

    private fun fetchMenuData() {
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        tempCal.add(Calendar.DAY_OF_MONTH, -7)
        val start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)
        
        tempCal.add(Calendar.DAY_OF_MONTH, 45)
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

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
        
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
        binding.rvCalendar.isNestedScrollingEnabled = false // Biarkan NestedScrollView yang handle scroll utama
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
