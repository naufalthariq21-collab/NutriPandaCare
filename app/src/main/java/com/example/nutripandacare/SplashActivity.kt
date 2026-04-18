package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY = 2500L
    }

    // Views sesuai ID di activity_splash.xml
    private lateinit var logoCard: CardView
    private lateinit var appName: TextView
    private lateinit var tagline: TextView
    private lateinit var bottomIndicator: LinearLayout
    private lateinit var loadingDots: LinearLayout
    private lateinit var topDecor: View
    private lateinit var circleDecor: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Fullscreen - sembunyikan status bar
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        initViews()
        startEntranceAnimations()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLogin()
        }, SPLASH_DELAY)
    }

    private fun initViews() {
        logoCard        = findViewById(R.id.logoCard)
        appName         = findViewById(R.id.appName)
        tagline         = findViewById(R.id.tagline)
        bottomIndicator = findViewById(R.id.bottomIndicator)
        loadingDots     = findViewById(R.id.loadingDots)
        topDecor        = findViewById(R.id.topDecor)
        circleDecor     = findViewById(R.id.circleDecor)
    }

    private fun startEntranceAnimations() {

        // Semua view mulai invisible
        logoCard.alpha        = 0f
        appName.alpha         = 0f
        tagline.alpha         = 0f
        bottomIndicator.alpha = 0f
        loadingDots.alpha     = 0f

        // 1. Logo: scale dari kecil + fade in
        val logoScale = ScaleAnimation(
            0.4f, 1.0f, 0.4f, 1.0f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        )
        val logoFade = AlphaAnimation(0f, 1f)
        val logoAnim = AnimationSet(true).apply {
            addAnimation(logoScale)
            addAnimation(logoFade)
            duration  = 650
            fillAfter = true
        }
        logoCard.startAnimation(logoAnim)

        // 2. App name: slide dari bawah + fade
        val nameSlide = TranslateAnimation(0f, 0f, 50f, 0f)
        val nameFade  = AlphaAnimation(0f, 1f)
        val nameAnim  = AnimationSet(true).apply {
            addAnimation(nameSlide)
            addAnimation(nameFade)
            duration    = 500
            startOffset = 450
            fillAfter   = true
        }
        appName.startAnimation(nameAnim)

        // 3. Tagline: slide + fade setelah nama
        val tagSlide = TranslateAnimation(0f, 0f, 30f, 0f)
        val tagFade  = AlphaAnimation(0f, 1f)
        val tagAnim  = AnimationSet(true).apply {
            addAnimation(tagSlide)
            addAnimation(tagFade)
            duration    = 450
            startOffset = 650
            fillAfter   = true
        }
        tagline.startAnimation(tagAnim)

        // 4. Loading dots: fade in
        val dotsFade = AlphaAnimation(0f, 1f).apply {
            duration    = 400
            startOffset = 900
            fillAfter   = true
        }
        loadingDots.startAnimation(dotsFade)

        // 5. Bottom indicator: fade in terakhir
        val bottomFade = AlphaAnimation(0f, 1f).apply {
            duration    = 400
            startOffset = 1100
            fillAfter   = true
        }
        bottomIndicator.startAnimation(bottomFade)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // Disable back di splash
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intentionally empty
    }
}