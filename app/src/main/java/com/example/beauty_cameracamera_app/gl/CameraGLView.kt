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

        setEGLContextClientVersion(2)

        setRenderer(renderer)

        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun release() {
        renderer?.release()
    }
}
