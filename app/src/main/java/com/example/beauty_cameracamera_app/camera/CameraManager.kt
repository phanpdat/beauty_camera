package com.example.beauty_cameracamera_app.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.beauty_cameracamera_app.ai.FaceMeshHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    fun startCamera(
        lifecycleOwner: LifecycleOwner, 
        surfaceTexture: SurfaceTexture,
        faceMeshListener: FaceMeshHelper.FaceMeshListener
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        var faceMeshHelper: FaceMeshHelper? = null
        try {
            faceMeshHelper = FaceMeshHelper(context, faceMeshListener)
            Log.i("CameraManager", "AI FaceMesh đã sẵn sàng!")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("CameraManager", "MediaPipe không hỗ trợ kiến trúc này (x86_64 Emulator). Camera vẫn hoạt động nhưng không có AI.", e)
        } catch (e: Exception) {
            Log.w("CameraManager", "Không thể khởi tạo AI: ${e.message}", e)
        }

        val aiHelper = faceMeshHelper // Capture cho lambda

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()

                .build().also {
                    it.setSurfaceProvider { request ->
                        val surface = Surface(surfaceTexture)
                        request.provideSurface(surface, ContextCompat.getMainExecutor(context)) { }
                    }
                }

            // Chỉ thiết lập AI Analysis nếu MediaPipe khả dụng
            if (aiHelper != null) {
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                   
                    .build().also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap() 
                            aiHelper.detectAsync(bitmap)
                            imageProxy.close()
                        }
                    }
            }

            try {
                cameraProvider?.unbindAll()
                if (imageAnalysis != null) {
                    // Chế độ đầy đủ: Camera + AI
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalysis
                    )
                } else {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraManager", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

//    fun switchCamera(lifecycleOwner: LifecycleOwner, surfaceTexture: SurfaceTexture) {
//        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
//            CameraSelector.DEFAULT_BACK_CAMERA
//        } else {
//            CameraSelector.DEFAULT_FRONT_CAMERA
//        }
//        startCamera(lifecycleOwner, surfaceTexture)
//    }

    fun release() {
        cameraProvider?.unbindAll()
    }
}
