package com.example.beauty_cameracamera_app.ai

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult


//Chịu trách nhiệm nhận diện 468 điểm mốc trên khuôn mặt bằng MediaPipe.

class FaceMeshHelper(
    private val context: Context,
    private val listener: FaceMeshListener
) {

    private var faceLandmarker: FaceLandmarker? = null

    companion object {
        private const val TAG = "FaceMeshHelper"
        const val MODEL_PATH = "face_landmarker.task"
    }

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setDelegate(Delegate.CPU) // Chạy trên CPU để ổn định nhất trên mọi thiết bị (kể cả máy ảo)
            .setModelAssetPath(MODEL_PATH) // File model trong thư mục assets

        try {
            val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.LIVE_STREAM) // Chuyển sang LIVE_STREAM để dùng Listener 
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener(this::returnExtractionResult) 
                .setErrorListener(this::returnDetectionError)

            val options = optionsBuilder.build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("FaceMeshHelper", "MediaPipe failed to initialize: ${e.message}")
        }
    }


    //Xử lý khung hình từ Camera
    fun detectAsync(bitmap: Bitmap) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        // Cần truyền thêm Timestamp ở chế độ LIVE_STREAM 
        faceLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
    }
    
    // Lấy 468 điểm landmarks
    private fun returnExtractionResult(result: FaceLandmarkerResult, input: MPImage) {
        if (result.faceLandmarks().isNotEmpty()) { 
            val landmarks = result.faceLandmarks()[0]
            listener.onResults(landmarks)
        } else {
            listener.onEmpty()
        }
    }

    private fun returnDetectionError(error: RuntimeException) {
        Log.e("FaceMeshHelper", "Detection Error: ${error.message}")
    }

    interface FaceMeshListener {
        fun onResults(landmarks: List<NormalizedLandmark>)
        fun onEmpty()
    }
}
