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
import com.example.beauty_cameracamera_app.gl.CameraRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
        lifecycleScope.launch {
            viewModel.intensity.collectLatest { valIntensity ->
                renderer?.intensity = valIntensity
            }
        }
        lifecycleScope.launch {
            viewModel.beautyLevel.collectLatest { valBeauty ->
                renderer?.beautyLevel = valBeauty
            }
        }
    }

    private fun setupIntensitySeekBar() {
        // Initial setup
        binding.intensitySeekBar.progress = (viewModel.intensity.value * 100).toInt()
        binding.beautySeekBar.progress = (viewModel.beautyLevel.value * 100).toInt()

        // Filter Intensity
        binding.intensitySeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.updateIntensity(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Beauty Level
        binding.beautySeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.updateBeautyLevel(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
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
        renderer = CameraRenderer(this) { surfaceTexture ->
            runOnUiThread {
                viewModel.startCamera(this, surfaceTexture)
            }
        }
        binding.cameraGLView.init(renderer!!)
    }

    // ========== Capture ==========

    private fun setupCaptureButton() {
        binding.btnCapture.setOnClickListener {
            renderer?.capture { bitmap ->
                // Callback chạy trên GL thread → chuyển về Main thread để save
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
                // Android 10+ dùng MediaStore
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
                // Android 9 trở xuống
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

    // ========== Filter List ==========

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