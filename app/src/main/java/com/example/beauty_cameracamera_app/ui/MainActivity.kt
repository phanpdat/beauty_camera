package com.example.beauty_cameracamera_app.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.beauty_cameracamera_app.databinding.ActivityMainBinding
import com.example.beauty_cameracamera_app.filter.Filter
import com.example.beauty_cameracamera_app.filter.FilterList
import androidx.lifecycle.lifecycleScope
import com.example.beauty_cameracamera_app.ai.FaceMeshHelper
import com.example.beauty_cameracamera_app.gl.CameraRenderer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CameraViewModel by viewModels()
    private var renderer: CameraRenderer? = null

    private val filters = FilterList.getFilters()
    private var selectedFilterView: TextView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initGLView()    
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        checkPermissions()
        setupFilterList()
        setupCaptureButton()
        setupIntensitySeekBar()
        observeViewModel()
    }

    private fun observeViewModel() {
        // Observe filter intensity
        lifecycleScope.launch {
            viewModel.filterIntensity.collect { intensity ->
                renderer?.intensity = intensity
            }
        }
    }

    private fun setupIntensitySeekBar() {
        // Kiểm tra xem thiết bị có hỗ trợ AI MediaPipe không (Máy ảo x86/x86_64 thường không hỗ trợ) 🛰️🛡️
        val isAiSupported = Build.SUPPORTED_ABIS.any { it.contains("arm") }
        
        if (!isAiSupported) {
            binding.noseSlimmingSeekBar.isEnabled = false
            binding.noseSlimmingSeekBar.alpha = 0.5f
            binding.chinSlimmingSeekBar.isEnabled = false
            binding.chinSlimmingSeekBar.alpha = 0.5f
            binding.eyeSizeSeekBar.isEnabled = false
            binding.eyeSizeSeekBar.alpha = 0.5f
            binding.lipSizeSeekBar.isEnabled = false
            binding.lipSizeSeekBar.alpha = 0.5f
            Toast.makeText(this, "🛡️ Chế độ AI chỉ hoạt động trên điện thoại thật (ARM).", Toast.LENGTH_LONG).show()
        }

        // Filter Intensity
        binding.intensitySeekBar.progress = (viewModel.filterIntensity.value * 100).toInt()
        binding.intensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.updateFilterIntensity(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // AI Nose Slimming 👃✨
        binding.noseSlimmingSeekBar.progress = 50 // Mặc định ở giữa (Không thay đổi)
        binding.noseSlimmingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isAiSupported) {
                    // Ánh xạ từ (0-100) sang (-1.0 đến 1.0) 📐✨
                    // 50 -> 0.0 (Bình thường), 0 -> -1.0 (Nhỏ lại), 100 -> 1.0 (To ra)
                    val amount = (progress - 50f) / 50f
                    renderer?.noseSlimming = amount
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // AI V-Line Chin 🦴✨
        binding.chinSlimmingSeekBar.progress = 50 
        binding.chinSlimmingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isAiSupported) {
                    val amount = (progress - 50f) / 50f
                    renderer?.chinSlimming = amount
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // AI Big Eyes 👀✨
        binding.eyeSizeSeekBar.progress = 0 
        binding.eyeSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isAiSupported) {
                    val amount = progress / 100f
                    renderer?.eyeSize = amount
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // AI Fuller Lips 👄✨
        binding.lipSizeSeekBar.progress = 50 
        binding.lipSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isAiSupported) {
                    val amount = (progress - 50f) / 50f
                    renderer?.lipSize = amount
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initGLView()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initGLView() {
        renderer = CameraRenderer(this) { st ->
            runOnUiThread {
                viewModel.startCamera(this, st, object : FaceMeshHelper.FaceMeshListener {
                    override fun onResults(landmarks: List<NormalizedLandmark>) {
                        // Lấy tọa độ các mốc
                        val noseTip = landmarks[1]
                        val chinTip = landmarks[152]
                        val leftEye = landmarks[159]
                        val rightEye = landmarks[386]
                        val upperLip = landmarks[13]

                        renderer?.noseCenter = Pair(noseTip.x(), noseTip.y())
                        renderer?.chinCenter = Pair(chinTip.x(), chinTip.y())
                        renderer?.leftEyeCenter = Pair(leftEye.x(), leftEye.y())
                        renderer?.rightEyeCenter = Pair(rightEye.x(), rightEye.y())
                        renderer?.lipCenter = Pair(upperLip.x(), upperLip.y())
                    }

                    override fun onEmpty() {
                        renderer?.noseCenter = Pair(0.5f, 0.5f)
                        renderer?.chinCenter = Pair(0.5f, 0.5f)
                        renderer?.leftEyeCenter = Pair(0.5f, 0.5f)
                        renderer?.rightEyeCenter = Pair(0.5f, 0.5f)
                        renderer?.lipCenter = Pair(0.5f, 0.5f)
                    }
                })
            }
        }
        binding.cameraGLView.init(renderer!!)
    }

    // ========== Capture ==========

    private fun setupCaptureButton() {
        binding.btnCapture.setOnClickListener {
            renderer?.capture { bitmap ->
                runOnUiThread {
                    saveToGallery(bitmap)
                }
            }
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        try {
            val fileName = "BeautyCamera_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BeautyCamera")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BeautyCamera")
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
            }

            bitmap.recycle()
            Toast.makeText(this, "📸 Đã lưu ảnh!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupFilterList() {
        val container = binding.filterContainer

        filters.forEach { filter ->
            val textView = createFilterButton(filter)
            container.addView(textView)

            if (filter.id == 0) {
                selectFilter(textView, filter)
            }
        }
    }

    private fun createFilterButton(filter: Filter): TextView {
        val tv = TextView(this).apply {
            text = filter.name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dp(4), 0, dp(4), 0)
            layoutParams = params

            background = getFilterBackground(false)

            setOnClickListener {
                selectFilter(this, filter)
            }
        }
        return tv
    }

    private fun selectFilter(view: TextView, filter: Filter) {
        selectedFilterView?.background = getFilterBackground(false)
        selectedFilterView?.setTextColor(Color.WHITE)

        view.background = getFilterBackground(true)
        view.setTextColor(Color.parseColor("#FFD700"))
        selectedFilterView = view

        renderer?.setFilter(filter)
    }

    private fun getFilterBackground(selected: Boolean): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            if (selected) {
                setColor(Color.parseColor("#44FFD700"))
                setStroke(dp(1), Color.parseColor("#FFD700"))
            } else {
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), Color.TRANSPARENT)
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.cameraGLView.onResume()
    }

    override fun onPause() {
        binding.cameraGLView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.cameraGLView.release()
        super.onDestroy()
    }
}