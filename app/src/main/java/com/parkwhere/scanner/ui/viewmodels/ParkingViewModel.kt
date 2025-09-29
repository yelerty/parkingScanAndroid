package com.parkwhere.scanner.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkwhere.scanner.data.BlacklistItem
import com.parkwhere.scanner.data.ParkingDatabase
import com.parkwhere.scanner.data.ParkingRecord
import com.parkwhere.scanner.utils.ImageProcessor
import com.parkwhere.scanner.utils.OCRProcessor
import com.parkwhere.scanner.utils.RegexDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ParkingViewModel : ViewModel() {

    private val _parkingRecords = MutableLiveData<List<ParkingRecord>>()
    val parkingRecords: LiveData<List<ParkingRecord>> = _parkingRecords

    private val _isScanning = MutableLiveData<Boolean>()
    val isScanning: LiveData<Boolean> = _isScanning

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private lateinit var database: ParkingDatabase
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var ocrProcessor: OCRProcessor
    private lateinit var regexDetector: RegexDetector
    private lateinit var sharedPreferences: SharedPreferences

    private var blacklistedPaths = mutableSetOf<String>()

    suspend fun initialize(context: Context) {
        database = ParkingDatabase.getDatabase(context)
        imageProcessor = ImageProcessor(context)
        ocrProcessor = OCRProcessor()
        regexDetector = RegexDetector()
        sharedPreferences = context.getSharedPreferences("parking_scanner", Context.MODE_PRIVATE)

        // 기존 기록 로드
        loadExistingRecords()

        // 블랙리스트 로드
        loadBlacklist()

        // 정규식 테스트 (디버그 모드에서)
        regexDetector.testParkingCodes()
    }

    private suspend fun loadExistingRecords() {
        try {
            val records = database.parkingDao().getAllRecords()
            _parkingRecords.postValue(records)
            println("✅ ${records.size}개의 기존 주차 기록 로드됨")
        } catch (e: Exception) {
            _errorMessage.postValue("기존 기록 로드 실패: ${e.message}")
        }
    }

    private suspend fun loadBlacklist() {
        try {
            // 30일 이상 된 블랙리스트 항목 제거
            val thirtyDaysAgo = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000)
            database.blacklistDao().removeOldBlacklistItems(thirtyDaysAgo)

            val blacklistItems = database.blacklistDao().getAllBlacklistItems()
            blacklistedPaths = blacklistItems.map { it.imagePath }.toMutableSet()
            println("✅ ${blacklistedPaths.size}개의 블랙리스트 항목 로드됨")
        } catch (e: Exception) {
            println("❌ 블랙리스트 로드 실패: ${e.message}")
        }
    }

    fun performInitialScanIfNeeded() {
        viewModelScope.launch {
            val existingRecords = database.parkingDao().getRecentRecords(5)
            if (existingRecords.isEmpty()) {
                performScan(targetCount = 5)
            }
        }
    }

    fun performManualScan() {
        viewModelScope.launch {
            performScan(targetCount = 10)
        }
    }

    fun refreshRecords() {
        viewModelScope.launch {
            performScan(targetCount = 5)
        }
    }

    private suspend fun performScan(targetCount: Int) = withContext(Dispatchers.IO) {
        _isScanning.postValue(true)
        _errorMessage.postValue(null)

        try {
            val images = imageProcessor.getRecentImages(limit = 200)
            val foundRecords = mutableListOf<ParkingRecord>()
            val userLicensePlate = getUserLicensePlate()

            println("📱 ${images.size}개 이미지 스캔 시작, 목표: ${targetCount}개 주차 기록")

            for ((index, imageInfo) in images.withIndex()) {
                if (foundRecords.size >= targetCount) break

                // 스크린샷 스킵
                if (imageInfo.isScreenshot) {
                    println("⏭️ [${index + 1}/${images.size}] 스킵: 스크린샷")
                    continue
                }

                // GPS 정보 없는 사진 스킵
                if (imageInfo.location == null) {
                    println("⏭️ [${index + 1}/${images.size}] 스킵: GPS 정보 없음")
                    continue
                }

                // 블랙리스트 스킵
                if (blacklistedPaths.contains(imageInfo.path)) {
                    println("⏭️ [${index + 1}/${images.size}] 스킵: 블랙리스트")
                    continue
                }

                println("🔍 [${index + 1}/${images.size}] 처리 중: ${imageInfo.path}")

                try {
                    val bitmap = imageProcessor.loadBitmap(imageInfo.uri, maxSize = 800)
                    if (bitmap == null) {
                        println("❌ [${index + 1}/${images.size}] 스킵: 비트맵 로드 실패")
                        continue
                    }

                    val lines = ocrProcessor.recognizeLines(bitmap)
                    if (lines.isEmpty()) {
                        println("❌ [${index + 1}/${images.size}] 스킵: OCR 텍스트 없음")
                        continue
                    }

                    var detectedCode: String? = null

                    // 1. 먼저 주차장 시설 코드를 찾습니다
                    val parkingCode = regexDetector.firstMatch(lines)
                    if (parkingCode != null && parkingCode.length <= 7 && parkingCode.length >= 2) {
                        detectedCode = parkingCode
                        println("✅ [${index + 1}/${images.size}] 주차장 시설 코드 발견: $parkingCode")
                    }
                    // 2. 주차 코드가 없으면 번호판을 찾습니다
                    else {
                        val plate = regexDetector.firstLicensePlateMatch(lines)
                        if (plate != null && plate.length in 6..10) {
                            // 사용자 번호판이 등록되어 있다면 매치 확인
                            if (!userLicensePlate.isNullOrEmpty()) {
                                if (plate.endsWith(userLicensePlate)) {
                                    detectedCode = plate
                                    println("✅ [${index + 1}/${images.size}] 사용자 번호판과 매치: $plate")
                                } else {
                                    println("❌ [${index + 1}/${images.size}] 사용자 번호판과 매치하지 않음: $plate vs $userLicensePlate")
                                }
                            } else {
                                // 사용자 번호판이 없다면 모든 번호판 허용
                                detectedCode = plate
                                println("✅ [${index + 1}/${images.size}] 번호판 발견: $plate")
                            }
                        } else {
                            println("❌ [${index + 1}/${images.size}] 스킵: 주차 코드나 번호판 인식되지 않음")
                            continue
                        }
                    }

                    if (detectedCode != null) {
                        val record = ParkingRecord(
                            code = detectedCode,
                            createdAt = imageInfo.dateAdded,
                            latitude = imageInfo.location.latitude,
                            longitude = imageInfo.location.longitude,
                            imagePath = imageInfo.path
                        )

                        foundRecords.add(record)
                        println("✅ [${index + 1}/${images.size}] 주차 기록 추가: $detectedCode (총 ${foundRecords.size}개)")
                    }

                } catch (e: Exception) {
                    println("❌ [${index + 1}/${images.size}] 스킵: 이미지 처리 오류 - ${e.message}")
                    continue
                }
            }

            // 결과 저장 및 업데이트
            if (foundRecords.isNotEmpty()) {
                for (record in foundRecords) {
                    database.parkingDao().insertRecord(record)
                }
                println("✅ 스캔 완료: ${foundRecords.size}개 주차 기록 저장됨")
            } else {
                println("⚠️ 스캔 완료: 주차 기록을 찾을 수 없음")
            }

            // UI 업데이트
            loadExistingRecords()

        } catch (e: Exception) {
            _errorMessage.postValue("스캔 중 오류 발생: ${e.message}")
            println("❌ 스캔 오류: ${e.message}")
        } finally {
            _isScanning.postValue(false)
        }
    }

    fun deleteRecord(record: ParkingRecord) {
        viewModelScope.launch {
            try {
                database.parkingDao().deleteRecord(record)
                loadExistingRecords()
            } catch (e: Exception) {
                _errorMessage.postValue("기록 삭제 실패: ${e.message}")
            }
        }
    }

    fun addToBlacklist(imagePath: String) {
        viewModelScope.launch {
            try {
                val blacklistItem = BlacklistItem(imagePath = imagePath)
                database.blacklistDao().insertBlacklistItem(blacklistItem)
                blacklistedPaths.add(imagePath)
                println("✅ 블랙리스트에 추가: $imagePath")
            } catch (e: Exception) {
                _errorMessage.postValue("블랙리스트 추가 실패: ${e.message}")
            }
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            try {
                database.parkingDao().clearAllRecords()
                _parkingRecords.postValue(emptyList())
                println("✅ 모든 기록 삭제 완료")

                // 전체 스캔 시작
                println("🚀 전체 스캔 시작...")
                performScan(targetCount = 20)
            } catch (e: Exception) {
                _errorMessage.postValue("기록 삭제 실패: ${e.message}")
            }
        }
    }

    fun getUserLicensePlate(): String? {
        return sharedPreferences.getString("user_license_plate", null)
    }

    fun setUserLicensePlate(plate: String) {
        sharedPreferences.edit()
            .putString("user_license_plate", plate)
            .apply()
    }

    fun cleanup() {
        ocrProcessor.close()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}