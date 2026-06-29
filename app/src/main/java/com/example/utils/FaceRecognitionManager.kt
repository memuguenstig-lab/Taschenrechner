package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FaceRecognitionManager(private val context: Context) {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .build()
    private val detector = FaceDetection.getClient(options)

    suspend fun detectFace(bitmap: Bitmap): Boolean = suspendCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces ->
                continuation.resume(faces.isNotEmpty())
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }
}
