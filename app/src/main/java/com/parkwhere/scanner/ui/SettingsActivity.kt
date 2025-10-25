package com.parkwhere.scanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.parkwhere.scanner.R
import com.parkwhere.scanner.databinding.ActivitySettingsBinding
import com.parkwhere.scanner.utils.LocalizationManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        setupUI()
        loadUserLicensePlate()
        updateLanguageButton()
    }

    private fun setupUI() {
        // 번호판 저장 버튼
        binding.btnSavePlate.setOnClickListener {
            val plate = binding.etLicensePlate.text.toString().trim()
            if (plate.isNotEmpty()) {
                saveUserLicensePlate(plate)
                Toast.makeText(this, R.string.license_plate_saved, Toast.LENGTH_SHORT).show()
            }
        }

        // 언어 선택 버튼
        binding.btnLanguage.setOnClickListener {
            showLanguageSelector()
        }

        // 사용 방법 보기 버튼
        binding.btnViewGuide.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        // 모든 기록 삭제 버튼
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }
    }

    private fun showLanguageSelector() {
        val languages = LocalizationManager.supportedLanguages
        val currentLanguage = LocalizationManager.getCurrentLanguage(this)
        val languageNames = languages.map { it.displayName }.toTypedArray()
        val currentIndex = languages.indexOfFirst { it.code == currentLanguage.code }

        AlertDialog.Builder(this)
            .setTitle(R.string.language_setting)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                LocalizationManager.setLanguage(this, selectedLanguage)
                LocalizationManager.updateConfiguration(this)

                // 액티비티 재시작하여 언어 적용
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateLanguageButton() {
        val currentLanguage = LocalizationManager.getCurrentLanguage(this)
        binding.btnLanguage.text = "${getString(R.string.language)}: ${currentLanguage.displayName}"
    }

    private fun loadUserLicensePlate() {
        val prefs = getSharedPreferences("parking_scanner", Context.MODE_PRIVATE)
        val savedPlate = prefs.getString("user_license_plate", "")
        binding.etLicensePlate.setText(savedPlate)
    }

    private fun saveUserLicensePlate(plate: String) {
        val prefs = getSharedPreferences("parking_scanner", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_license_plate", plate)
            .apply()
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_records)
            .setMessage(R.string.clear_all_confirmation)
            .setPositiveButton(R.string.confirm) { _, _ ->
                // MainActivity에 알리기 위해 result 설정
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}