package com.example.beauty_cameracamera_app.filter

import android.graphics.Bitmap

/**
 * Model cho mỗi filter
 */
data class Filter(
    val id: Int,
    val name: String,
    val type: FilterType
)

sealed class FilterType {
    // Filter dùng LUT: ưu tiên load assetPath, nếu lỗi dùng lutBitmap
    data class LutFilter(
        val assetPath: String? = null,
        val lutBitmap: (() -> android.graphics.Bitmap)? = null
    ) : FilterType()
}

/**
 * Tạo danh sách filter có sẵn
 */
object FilterList {
    fun getFilters(): List<Filter> = listOf(
        Filter(0, "Scandi 01", FilterType.LutFilter("luts/vertopal.com_TDCat_Scandi_Cool_01.png", { LutGenerator.generateIdentity() })),
        Filter(1, "Scandi 02", FilterType.LutFilter("luts/vertopal.com_TDCat_Scandi_Cool_02.png", { LutGenerator.generateIdentity() })),
        Filter(2, "Cool",     FilterType.LutFilter("luts/cool.png",     { LutGenerator.generateCool() })),
        Filter(3, "Pink",     FilterType.LutFilter("luts/pink.png",     { LutGenerator.generatePink() })),
        Filter(4, "Vintage",  FilterType.LutFilter("luts/vintage.png",  { LutGenerator.generateVintage() })),
        Filter(5, "Fresh",    FilterType.LutFilter("luts/fresh.png",    { LutGenerator.generateFresh() })),
        Filter(6, "Sunset",   FilterType.LutFilter("luts/sunset.png",   { LutGenerator.generateSunset() })),
        Filter(7, "Gray",     FilterType.LutFilter("luts/gray.png",     { LutGenerator.generateGrayscale() })),
        Filter(8, "Invert",   FilterType.LutFilter("luts/invert.png",   { LutGenerator.generateInvert() })),
        Filter(9, "Sepia",    FilterType.LutFilter("luts/sepia.png",    { LutGenerator.generateSepia() })),
    )
}
