package com.example.beauty_cameracamera_app.ui

import android.graphics.SurfaceTexture
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.example.beauty_cameracamera_app.camera.CameraManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager
) : ViewModel() {

    private val _filterIntensity = MutableStateFlow(0.8f)
    val filterIntensity = _filterIntensity.asStateFlow()

    fun updateFilterIntensity(value: Float) {
        _filterIntensity.value = value
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceTexture: SurfaceTexture) {
        cameraManager.startCamera(lifecycleOwner, surfaceTexture)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, surfaceTexture: SurfaceTexture) {
        cameraManager.switchCamera(lifecycleOwner, surfaceTexture)
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}
