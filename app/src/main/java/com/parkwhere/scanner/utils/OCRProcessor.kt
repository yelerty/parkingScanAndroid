package com.parkwhere.scanner.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRProcessor {

    private val textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /**
     * 이미지에서 텍스트를 인식하여 라인 단위로 반환
     */
    suspend fun recognizeLines(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text.trim()
                        if (text.isNotEmpty()) {
                            lines.add(text)
                        }
                    }
                }

                println("📝 OCR 결과: ${lines.size}개 라인 인식됨")
                if (lines.isNotEmpty()) {
                    println("📝 인식된 텍스트: ${lines.take(3).joinToString(", ")}${if (lines.size > 3) "..." else ""}")
                }

                continuation.resume(lines)
            }
            .addOnFailureListener { exception ->
                println("❌ OCR 실패: ${exception.message}")
                continuation.resumeWithException(exception)
            }
    }

    /**
     * 이미지에서 모든 텍스트를 하나의 문자열로 반환
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text.trim()
                println("📝 OCR 전체 텍스트: $fullText")
                continuation.resume(fullText)
            }
            .addOnFailureListener { exception ->
                println("❌ OCR 실패: ${exception.message}")
                continuation.resumeWithException(exception)
            }
    }

    /**
     * 리소스 정리
     */
    fun close() {
        textRecognizer.close()
    }
}