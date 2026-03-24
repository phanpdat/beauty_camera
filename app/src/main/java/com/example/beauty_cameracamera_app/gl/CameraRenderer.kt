package com.example.beauty_cameracamera_app.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.graphics.SurfaceTexture
import android.util.Log
import com.example.beauty_cameracamera_app.filter.Filter
import com.example.beauty_cameracamera_app.filter.FilterType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(
    private val context: Context,
    private val onSurfaceTextureReady: (SurfaceTexture) -> Unit
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "CameraRenderer"
    }

    // OES texture ID cho camera
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null

    // LUT texture ID
    private var lutTextureId = 0

    // Shader programs
    private var shaderProgram = 0

    // Vertex shader source (cache)
    private var vertexShaderSource = ""
    private var lutFragmentSource = ""

    // Attribute & Uniform locations
    private var aPositionLoc = 0
    private var aTexCoordLoc = 0
    private var uTextureLoc = 0
    private var uLutTextureLoc = -1
    private var uIntensityLoc = -1
    private var uBeautyLevelLoc = -1

    // Intensity cho filter
    @Volatile
    var intensity: Float = 0.8f

    // Beauty level
    @Volatile
    var beautyLevel: Float = 0.5f

    @Volatile
    private var pendingFilter: Filter? = null
    private var currentFilter: Filter? = null

    // Capture
    @Volatile
    private var captureRequested = false
    private var onCaptureCallback: ((Bitmap) -> Unit)? = null
    private var viewportWidth = 0
    private var viewportHeight = 0

    // Vertex data: full-screen quad
    private val vertexData = floatArrayOf(
        -1f, -1f, 0f, 0f,
         1f, -1f, 1f, 0f,
        -1f,  1f, 0f, 1f,
         1f,  1f, 1f, 1f,
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertexData)
        .also { it.position(0) }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // Tạo OES texture
        oesTextureId = createOESTexture()

        // Tạo SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId)

        // Cache shader sources
        vertexShaderSource = loadShaderFromAssets("shaders/vertex_shader.glsl")
        lutFragmentSource = loadShaderFromAssets("shaders/lut_fragment_shader.glsl")

        // Compile shader mặc định (dùng LUT shader làm base)
        shaderProgram = createShaderProgram(vertexShaderSource, lutFragmentSource)
        updateLocations()

        // Load Identity LUT ban đầu
        val identity = com.example.beauty_cameracamera_app.filter.LutGenerator.generateIdentity()
        loadLutTexture(identity)

        // Gửi SurfaceTexture cho CameraManager
        onSurfaceTextureReady(surfaceTexture!!)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        // Kiểm tra đổi filter
        pendingFilter?.let { newFilter ->
            applyFilter(newFilter)
            currentFilter = newFilter
            pendingFilter = null
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Cập nhật frame camera
        surfaceTexture?.updateTexImage()

        // Sử dụng shader
        GLES20.glUseProgram(shaderProgram)

        // Bind camera texture (unit 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        // Bind LUT texture (unit 1)
        if (lutTextureId != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
            if (uLutTextureLoc >= 0) {
                GLES20.glUniform1i(uLutTextureLoc, 1)
            }
        }

        // Set intensity
        if (uIntensityLoc >= 0) {
            GLES20.glUniform1f(uIntensityLoc, intensity)
        }

        // Set beauty level
        if (uBeautyLevelLoc >= 0) {
            GLES20.glUniform1f(uBeautyLevelLoc, beautyLevel)
        }

        // Set vertex data
        val stride = 4 * 4

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        // Vẽ
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)

        // Capture nếu được yêu cầu
        if (captureRequested) {
            captureRequested = false
            val bitmap = readPixelsToBitmap(viewportWidth, viewportHeight)
            onCaptureCallback?.invoke(bitmap)
            onCaptureCallback = null
        }
    }

    // ========== Public API ==========

    fun setFilter(filter: Filter) {
        pendingFilter = filter
    }

    /**
     * Yêu cầu capture frame hiện tại (gọi từ UI thread)
     * Callback trả về Bitmap trên GL thread
     */
    fun capture(callback: (Bitmap) -> Unit) {
        onCaptureCallback = callback
        captureRequested = true
    }

    private fun readPixelsToBitmap(width: Int, height: Int): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        buffer.order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        buffer.rewind()

        // OpenGL đọc từ dưới lên → cần flip
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        // Flip dọc
        val matrix = android.graphics.Matrix()
        matrix.postScale(1f, -1f, width / 2f, height / 2f)
        val flipped = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        bitmap.recycle()

        return flipped
    }

    // ========== Private Helpers ==========

    private fun applyFilter(filter: Filter) {
        val type = filter.type as? FilterType.LutFilter ?: return
        
        // Chỉ cần nạp lại Texture, không cần biên dịch lại Shader (Tối ưu cực mạnh)
        val bitmap = tryLoadLut(type)
        loadLutTexture(bitmap)
        
        Log.d(TAG, "Switched LUT filter: ${filter.name}")
    }

    private fun tryLoadLut(type: FilterType.LutFilter): Bitmap {
        // 1. Thử load từ Assets nếu có đường dẫn
        type.assetPath?.let { path ->
            try {
                context.assets.open(path).use { 
                    return android.graphics.BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not find asset: $path, falling back to generator")
            }
        }
        
        // 2. Fallback dùng code generator
        return type.lutBitmap?.invoke() 
            ?: com.example.beauty_cameracamera_app.filter.LutGenerator.generateIdentity()
    }

    private fun updateLocations() {
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture")
        uLutTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uLutTexture")
        uIntensityLoc = GLES20.glGetUniformLocation(shaderProgram, "uIntensity")
        uBeautyLevelLoc = GLES20.glGetUniformLocation(shaderProgram, "uBeautyLevel")
    }

    private fun loadLutTexture(bitmap: Bitmap) {
        // Xóa texture cũ
        if (lutTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
        }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        lutTextureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
    }

    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    private fun createShaderProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Shader link failed: $log")
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }

    private fun loadShaderFromAssets(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun release() {
        surfaceTexture?.release()
        if (shaderProgram != 0) GLES20.glDeleteProgram(shaderProgram)
        if (lutTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
    }
}
