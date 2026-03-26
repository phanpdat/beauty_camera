package com.example.beauty_cameracamera_app.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.beauty_cameracamera_app.filter.Filter
import com.example.beauty_cameracamera_app.filter.FilterList
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(
    private val context: Context,
    private val onSurfaceReady: (SurfaceTexture) -> Unit
) : GLSurfaceView.Renderer {

    private var shaderProgram = 0
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null

    // Locations Shader
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uTextureLoc = -1
    private var uSTMatrixLoc = -1
    private var uIntensityLoc = -1
    private var uScaleLoc = -1 
    private var uNoseCenterLoc = -1
    private var uNoseSlimmingLoc = -1
    private var uChinCenterLoc = -1
    private var uChinSlimmingLoc = -1
    private var uLeftEyeCenterLoc = -1
    private var uRightEyeCenterLoc = -1
    private var uEyeSizeLoc = -1
    private var uLipCenterLoc = -1
    private var uLipSizeLoc = -1

    // Handles Filter Params
    private var uBrightnessLoc = -1
    private var uContrastLoc = -1
    private var uSaturationLoc = -1
    private var uRedShiftLoc = -1
    private var uGreenShiftLoc = -1
    private var uBlueShiftLoc = -1
    private var uGammaLoc = -1

    // Handles Overlay Colors
    private var uOverlayColorLoc = -1
    private var uOverlayStrengthLoc = -1

    private val stMatrix = FloatArray(16)

    @Volatile
    var intensity: Float = 0.8f

    @Volatile
    var scale: Float = 1.0f 

    // THÔNG SỐ PHẪU THUẬT MŨI (AI) 👃✨
    @Volatile
    var noseCenter: Pair<Float, Float> = Pair(0.5f, 0.5f) 
    
    @Volatile
    var noseSlimming: Float = 0.0f // 0.0 là bình thường, 1.0 là thon gọn nhất

    // THÔNG SỐ CẰM, MẮT, MÔI (AI) 🦴👀👄
    @Volatile
    var chinCenter: Pair<Float, Float> = Pair(0.5f, 0.5f)
    @Volatile
    var chinSlimming: Float = 0.0f

    @Volatile
    var leftEyeCenter: Pair<Float, Float> = Pair(0.5f, 0.5f)
    @Volatile
    var rightEyeCenter: Pair<Float, Float> = Pair(0.5f, 0.5f)
    @Volatile
    var eyeSize: Float = 0.0f

    @Volatile
    var lipCenter: Pair<Float, Float> = Pair(0.5f, 0.5f)
    @Volatile
    var lipSize: Float = 0.0f

    @Volatile
    private var currentFilter: Filter = FilterList.getFilters()[0]

    private var captureNext = false
    private var captureCallback: ((Bitmap) -> Unit)? = null

    fun setFilter(filter: Filter) {
        currentFilter = filter
    }

    fun capture(callback: (Bitmap) -> Unit) {
        captureCallback = callback
        captureNext = true
    }

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(VERTS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(VERTS).apply { position(0) }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexSource = loadShaderFromAssets("shaders/vertex_shader.glsl")
        val fragmentSource = loadShaderFromAssets("shaders/filter_fragment_shader.glsl")
        shaderProgram = createShaderProgram(vertexSource, fragmentSource)
        updateLocations()

        oesTextureId = createOESTexture()
        val st = SurfaceTexture(oesTextureId)
        st.setDefaultBufferSize(1080, 1920) // Đảm bảo độ phân giải cao nhất cho Surface
        surfaceTexture = st
        onSurfaceReady(st)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        surfaceTexture?.updateTexImage()
        GLES20.glUseProgram(shaderProgram)

        surfaceTexture?.getTransformMatrix(stMatrix)
        GLES20.glUniformMatrix4fv(uSTMatrixLoc, 1, false, stMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        val p = currentFilter.params
        GLES20.glUniform1f(uBrightnessLoc, p.brightness)
        GLES20.glUniform1f(uContrastLoc, p.contrast)
        GLES20.glUniform1f(uSaturationLoc, p.saturation)
        GLES20.glUniform1f(uRedShiftLoc, p.redShift)
        GLES20.glUniform1f(uGreenShiftLoc, p.greenShift)
        GLES20.glUniform1f(uBlueShiftLoc, p.blueShift)
        GLES20.glUniform1f(uGammaLoc, p.gamma)
        GLES20.glUniform1f(uIntensityLoc, intensity)
        GLES20.glUniform1f(uScaleLoc, scale)
        
        // NẠP TỌA ĐỘ TỪ AI VÀO GPU 🧠✨
        GLES20.glUniform2f(uNoseCenterLoc, noseCenter.first, noseCenter.second)
        GLES20.glUniform1f(uNoseSlimmingLoc, noseSlimming)
        
        GLES20.glUniform2f(uChinCenterLoc, chinCenter.first, chinCenter.second)
        GLES20.glUniform1f(uChinSlimmingLoc, chinSlimming)
        
        GLES20.glUniform2f(uLeftEyeCenterLoc, leftEyeCenter.first, leftEyeCenter.second)
        GLES20.glUniform2f(uRightEyeCenterLoc, rightEyeCenter.first, rightEyeCenter.second)
        GLES20.glUniform1f(uEyeSizeLoc, eyeSize)
        
        GLES20.glUniform2f(uLipCenterLoc, lipCenter.first, lipCenter.second)
        GLES20.glUniform1f(uLipSizeLoc, lipSize)

        // CHỖ NÀY ĐÃ ĐƯỢC SỬA LẠI CHUẨN (GLES20.glUniform3f) ✨
        GLES20.glUniform3f(uOverlayColorLoc, p.overlayR, p.overlayG, p.overlayB)
        GLES20.glUniform1f(uOverlayStrengthLoc, p.overlayStrength)

        val stride = 4 * 4
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        if (captureNext) {
            val bitmap = createBitmapFromGL()
            captureCallback?.invoke(bitmap)
            captureNext = false
        }
    }

    private fun updateLocations() {
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture")
        uSTMatrixLoc = GLES20.glGetUniformLocation(shaderProgram, "uSTMatrix")
        uIntensityLoc = GLES20.glGetUniformLocation(shaderProgram, "uIntensity")
        uScaleLoc     = GLES20.glGetUniformLocation(shaderProgram, "uScale")
        
        uNoseCenterLoc = GLES20.glGetUniformLocation(shaderProgram, "uNoseCenter")
        uNoseSlimmingLoc = GLES20.glGetUniformLocation(shaderProgram, "uNoseSlimming")
        
        uChinCenterLoc = GLES20.glGetUniformLocation(shaderProgram, "uChinCenter")
        uChinSlimmingLoc = GLES20.glGetUniformLocation(shaderProgram, "uChinSlimming")
        
        uLeftEyeCenterLoc = GLES20.glGetUniformLocation(shaderProgram, "uLeftEye")
        uRightEyeCenterLoc = GLES20.glGetUniformLocation(shaderProgram, "uRightEye")
        uEyeSizeLoc = GLES20.glGetUniformLocation(shaderProgram, "uEyeSize")
        
        uLipCenterLoc = GLES20.glGetUniformLocation(shaderProgram, "uLipCenter")
        uLipSizeLoc = GLES20.glGetUniformLocation(shaderProgram, "uLipSize")

        uBrightnessLoc = GLES20.glGetUniformLocation(shaderProgram, "uBrightness")
        uContrastLoc = GLES20.glGetUniformLocation(shaderProgram, "uContrast")
        uSaturationLoc = GLES20.glGetUniformLocation(shaderProgram, "uSaturation")
        uRedShiftLoc = GLES20.glGetUniformLocation(shaderProgram, "uRedShift")
        uGreenShiftLoc = GLES20.glGetUniformLocation(shaderProgram, "uGreenShift")
        uBlueShiftLoc = GLES20.glGetUniformLocation(shaderProgram, "uBlueShift")
        uGammaLoc = GLES20.glGetUniformLocation(shaderProgram, "uGamma")

        uOverlayColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uOverlayColor")
        uOverlayStrengthLoc = GLES20.glGetUniformLocation(shaderProgram, "uOverlayStrength")
    }

    private fun createBitmapFromGL(): Bitmap {
        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        val w = viewport[2]
        val h = viewport[3]
        val buffer = ByteBuffer.allocateDirect(w * h * 4)
        buffer.order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        val matrix = android.graphics.Matrix()
        matrix.postScale(1f, -1f)
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        return textures[0]
    }

    private fun createShaderProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).apply {
            GLES20.glShaderSource(this, vertexSource)
            GLES20.glCompileShader(this)
        }
        val fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).apply {
            GLES20.glShaderSource(this, fragmentSource)
            GLES20.glCompileShader(this)
        }
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vs)
            GLES20.glAttachShader(this, fs)
            GLES20.glLinkProgram(this)
        }
    }

    private fun loadShaderFromAssets(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun release() {
        surfaceTexture?.release()
        if (shaderProgram != 0) GLES20.glDeleteProgram(shaderProgram)
    }

    companion object {
        private val VERTS = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )
    }
}
