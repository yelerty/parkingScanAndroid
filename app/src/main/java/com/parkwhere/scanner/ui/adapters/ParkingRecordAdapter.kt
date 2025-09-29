package com.parkwhere.scanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.parkwhere.scanner.R
import com.parkwhere.scanner.data.ParkingRecord
import com.parkwhere.scanner.databinding.ItemParkingRecordBinding
import java.text.SimpleDateFormat
import java.util.*

class ParkingRecordAdapter(
    private val onItemClick: (ParkingRecord) -> Unit,
    private val onDeleteClick: (ParkingRecord) -> Unit,
    private val onBlacklistClick: (ParkingRecord) -> Unit
) : ListAdapter<ParkingRecord, ParkingRecordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkingRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemParkingRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: ParkingRecord) {
            binding.apply {
                // 주차 코드 표시
                textParkingCode.text = record.code

                // 시간 표시 (iOS 앱과 동일한 형식)
                val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                textDateTime.text = dateFormat.format(record.createdAt)

                // 위치 표시
                if (record.latitude != null && record.longitude != null) {
                    textLocation.text = String.format("%.6f, %.6f", record.latitude, record.longitude)
                    textLocation.visibility = android.view.View.VISIBLE
                } else {
                    textLocation.visibility = android.view.View.GONE
                }

                // 썸네일 이미지 로드
                try {
                    Glide.with(imageThumbnail.context)
                        .load(record.imagePath)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .into(imageThumbnail)
                } catch (e: Exception) {
                    imageThumbnail.setImageResource(R.drawable.ic_image_error)
                }

                // 클릭 이벤트
                root.setOnClickListener {
                    onItemClick(record)
                }

                // 삭제 버튼
                buttonDelete.setOnClickListener {
                    onDeleteClick(record)
                }

                // 블랙리스트 버튼
                buttonBlacklist.setOnClickListener {
                    onBlacklistClick(record)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ParkingRecord>() {
        override fun areItemsTheSame(oldItem: ParkingRecord, newItem: ParkingRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ParkingRecord, newItem: ParkingRecord): Boolean {
            return oldItem == newItem
        }
    }
}