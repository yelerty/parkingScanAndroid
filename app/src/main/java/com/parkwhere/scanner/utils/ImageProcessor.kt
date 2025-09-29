package com.parkwhere.scanner.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class ImageInfo(
    val uri: Uri,
    val path: String,
    val dateAdded: Date,
    val location: Location? = null,
    val isScreenshot: Boolean = false
)

class ImageProcessor(private val context: Context) {

    /**
     * 최근 이미지들을 가져오기 (GPS 정보와 함께)
     */
    suspend fun getRecentImages(limit: Int = 200, afterDate: Date? = null): List<ImageInfo> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageInfo>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        var selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'"
        val selectionArgs = mutableListOf<String>()

        // 날짜 필터 추가
        afterDate?.let { date ->
            selection += " AND ${MediaStore.Images.Media.DATE_ADDED} > ?"
            selectionArgs.add((date.time / 1000).toString())
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            sortOrder
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val latColumn = c.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lngColumn = c.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idColumn)
                    val path = c.getString(dataColumn) ?: continue
                    val dateAdded = Date(c.getLong(dateColumn) * 1000)
                    val displayName = c.getString(nameColumn) ?: ""

                    // 스크린샷 판별
                    val isScreenshot = displayName.contains("screenshot", ignoreCase = true) ||
                            path.contains("screenshot", ignoreCase = true) ||
                            displayName.startsWith("Screenshot_")

                    // GPS 정보 읽기
                    var location: Location? = null
                    if (latColumn >= 0 && lngColumn >= 0) {
                        val lat = c.getDouble(latColumn)
                        val lng = c.getDouble(lngColumn)
                        if (lat != 0.0 && lng != 0.0) {
                            location = Location("").apply {
                                latitude = lat
                                longitude = lng
                            }
                        }
                    }

                    // EXIF에서 GPS 정보 읽기 (MediaStore에 없는 경우)
                    if (location == null) {
                        location = getLocationFromExif(path)
                    }

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    images.add(ImageInfo(
                        uri = uri,
                        path = path,
                        dateAdded = dateAdded,
                        location = location,
                        isScreenshot = isScreenshot
                    ))
                } catch (e: Exception) {
                    println("❌ 이미지 정보 읽기 실패: ${e.message}")
                }
            }
        }

        println("📱 총 ${images.size}개 이미지 로드됨")
        return@withContext images
    }

    /**
     * EXIF 데이터에서 GPS 정보 추출
     */
    private fun getLocationFromExif(imagePath: String): Location? {
        return try {
            val exif = ExifInterface(imagePath)
            val latLong = FloatArray(2)

            if (exif.getLatLong(latLong)) {
                Location("").apply {
                    latitude = latLong[0].toDouble()
                    longitude = latLong[1].toDouble()
                }
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 이미지를 비트맵으로 로드 (크기 조정)
     */
    suspend fun loadBitmap(uri: Uri, maxSize: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 크기 계산
            val scale = calculateInSampleSize(options, maxSize, maxSize)
            options.inSampleSize = scale
            options.inJustDecodeBounds = false

            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 회전 보정
            bitmap?.let { correctRotation(it, uri) }
        } catch (e: Exception) {
            println("❌ 비트맵 로드 실패: ${e.message}")
            null
        }
    }

    /**
     * 썸네일 비트맵 로드 (빠른 로딩용)
     */
    suspend fun loadThumbnail(uri: Uri, size: Int = 150): Bitmap? = withContext(Dispatchers.IO) {
        try {
            MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                ContentUris.parseId(uri),
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
            )
        } catch (e: Exception) {
            // 썸네일이 없으면 원본에서 작은 크기로 로드
            loadBitmap(uri, size)
        }
    }

    /**
     * 이미지 회전 보정
     */
    private fun correctRotation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }

                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * 적절한 샘플 크기 계산
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}