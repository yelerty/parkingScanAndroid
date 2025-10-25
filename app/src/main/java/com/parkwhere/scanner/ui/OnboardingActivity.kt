package com.parkwhere.scanner.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.parkwhere.scanner.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("parking_scanner", Context.MODE_PRIVATE)

        setupUI()
    }

    private fun setupUI() {
        binding.btnGotIt.setOnClickListener {
            // 온보딩 완료 표시
            sharedPreferences.edit()
                .putBoolean("has_seen_onboarding", true)
                .apply()

            // 액티비티 종료
            finish()
        }
    }

    companion object {
        /**
         * 온보딩을 표시해야 하는지 확인
         */
        fun shouldShowOnboarding(context: Context): Boolean {
            val prefs = context.getSharedPreferences("parking_scanner", Context.MODE_PRIVATE)
            return !prefs.getBoolean("has_seen_onboarding", false)
        }
    }
}
