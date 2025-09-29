package com.parkwhere.scanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.parkwhere.scanner.R
import com.parkwhere.scanner.databinding.ActivityMainBinding
import com.parkwhere.scanner.ui.adapters.ParkingRecordAdapter
import com.parkwhere.scanner.ui.viewmodels.ParkingViewModel
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ParkingViewModel by viewModels()
    private lateinit var adapter: ParkingRecordAdapter

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeApp()
        } else {
            Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        initializeAds()
        checkPermissions()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "주차 스캐너"

        // RecyclerView 설정
        adapter = ParkingRecordAdapter(
            onItemClick = { record ->
                // 지도 화면으로 이동
                if (record.latitude != null && record.longitude != null) {
                    val intent = Intent(this, MapActivity::class.java).apply {
                        putExtra("parking_record", record)
                    }
                    startActivity(intent)
                }
            },
            onDeleteClick = { record ->
                viewModel.deleteRecord(record)
            },
            onBlacklistClick = { record ->
                viewModel.addToBlacklist(record.imagePath)
                viewModel.deleteRecord(record)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRecords()
        }

        // Floating Action Button (수동 스캔)
        binding.fabScan.setOnClickListener {
            viewModel.performManualScan()
        }
    }

    private fun setupObservers() {
        // 주차 기록 관찰
        viewModel.parkingRecords.observe(this) { records ->
            adapter.submitList(records)
            binding.swipeRefresh.isRefreshing = false

            // 빈 상태 처리
            if (records.isEmpty()) {
                binding.emptyStateText.text = "주차 기록이 없습니다.\n아래 버튼을 눌러 스캔을 시작하세요."
            }
        }

        // 스캔 상태 관찰
        viewModel.isScanning.observe(this) { isScanning ->
            binding.progressBar.visibility = if (isScanning) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.scanStatusText.text = if (isScanning) {
                "스캔 중..."
            } else {
                ""
            }
        }

        // 에러 메시지 관찰
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeAds() {
        MobileAds.initialize(this) { initializationStatus ->
            println("✅ AdMob 초기화 완료")
        }

        // 배너 광고 로드
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun checkPermissions() {
        if (EasyPermissions.hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            initializeApp()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "이 앱은 사진과 위치 정보에 접근이 필요합니다.",
                PERMISSIONS_REQUEST_CODE,
                *REQUIRED_PERMISSIONS
            )
        }
    }

    private fun initializeApp() {
        lifecycleScope.launch {
            viewModel.initialize(this@MainActivity)
            viewModel.performInitialScanIfNeeded()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                viewModel.refreshRecords()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            initializeApp()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            Toast.makeText(this, "설정에서 권한을 허용해주세요", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}