package com.example.beauty_cameracamera_app.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class CameraGLView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var renderer: CameraRenderer? = null

    fun init(cameraRenderer: CameraRenderer) {
        renderer = cameraRenderer

        // OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // Renderer
        setRenderer(renderer)

        // Chỉ vẽ khi có frame mới (tiết kiệm pin)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun release() {
        renderer?.release()
    }
}
