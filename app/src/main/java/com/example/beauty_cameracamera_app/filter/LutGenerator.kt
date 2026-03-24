package com.example.beauty_cameracamera_app.filter

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Tạo LUT texture 512x512 (8x8 grid, mỗi ô 64x64)
 * mà không cần file ảnh bên ngoài.
 *
 * Mỗi hàm tạo ra một bảng màu LUT khác nhau = 1 filter.
 * Muốn thêm filter mới? → Thêm 1 hàm mới hoặc 1 file PNG.
 */
object LutGenerator {

    private const val LUT_SIZE = 64
    private const val TILE_COUNT = 8  // 8x8 tiles
    private const val BITMAP_SIZE = 512 // 64 * 8

    /**
     * Tạo Identity LUT (không đổi màu) — dùng làm base
     */
    fun generateIdentity(): Bitmap {
        return generateLUT { r, g, b -> Triple(r, g, b) }
    }

    /**
     * Filter Cream — tone kem sữa ấm áp
     */
    fun generateCream(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.min(255, (r * 1.1 + 10).toInt())
            val gg = Math.min(255, (g * 1.05 + 5).toInt())
            val bb = Math.max(0, (b * 0.9).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Cool — tone xanh lạnh
     */
    fun generateCool(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.max(0, (r * 0.9).toInt())
            val gg = Math.min(255, (g * 1.0).toInt())
            val bb = Math.min(255, (b * 1.15 + 10).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Pink — tone hồng nhẹ
     */
    fun generatePink(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.min(255, (r * 1.1 + 15).toInt())
            val gg = Math.max(0, (g * 0.95).toInt())
            val bb = Math.min(255, (b * 1.05 + 5).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Vintage — tone cổ điển phim
     */
    fun generateVintage(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.min(255, (r * 1.05 + 20).toInt())
            val gg = Math.min(255, (g * 0.95 + 10).toInt())
            val bb = Math.max(0, (b * 0.8).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Fresh — tone tươi sáng
     */
    fun generateFresh(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.min(255, (r * 1.05).toInt())
            val gg = Math.min(255, (g * 1.1 + 8).toInt())
            val bb = Math.min(255, (b * 1.05 + 5).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Sunset — tone hoàng hôn
     */
    fun generateSunset(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = Math.min(255, (r * 1.2 + 15).toInt())
            val gg = Math.min(255, (g * 0.95 + 5).toInt())
            val bb = Math.max(0, (b * 0.75).toInt())
            Triple(rr, gg, bb)
        }
    }

    /**
     * Filter Grayscale — đen trắng
     */
    fun generateGrayscale(): Bitmap {
        return generateLUT { r, g, b ->
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            Triple(gray, gray, gray)
        }
    }

    /**
     * Filter Invert — đảo màu
     */
    fun generateInvert(): Bitmap {
        return generateLUT { r, g, b ->
            Triple(255 - r, 255 - g, 255 - b)
        }
    }

    /**
     * Filter Sepia — màu hoài cổ
     */
    fun generateSepia(): Bitmap {
        return generateLUT { r, g, b ->
            val rr = (r * 0.393 + g * 0.769 + b * 0.189).toInt().coerceIn(0, 255)
            val gg = (r * 0.349 + g * 0.686 + b * 0.168).toInt().coerceIn(0, 255)
            val bb = (r * 0.272 + g * 0.534 + b * 0.131).toInt().coerceIn(0, 255)
            Triple(rr, gg, bb)
        }
    }

    // ========== Core Generator ==========

    /**
     * Tạo LUT bitmap 512x512 từ hàm biến đổi màu
     */
    private fun generateLUT(
        transform: (r: Int, g: Int, b: Int) -> Triple<Int, Int, Int>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(BITMAP_SIZE * BITMAP_SIZE)

        for (blue in 0 until LUT_SIZE) {
            // Vị trí ô trong grid 8x8
            val tileX = blue % TILE_COUNT
            val tileY = blue / TILE_COUNT

            for (green in 0 until LUT_SIZE) {
                for (red in 0 until LUT_SIZE) {
                    // Scale 0-63 → 0-255
                    val r = (red * 255) / (LUT_SIZE - 1)
                    val g = (green * 255) / (LUT_SIZE - 1)
                    val b = (blue * 255) / (LUT_SIZE - 1)

                    // Áp dụng biến đổi màu
                    val (newR, newG, newB) = transform(r, g, b)

                    // Vị trí pixel trong bitmap
                    val x = tileX * LUT_SIZE + red
                    val y = tileY * LUT_SIZE + green

                    pixels[y * BITMAP_SIZE + x] = Color.argb(
                        255,
                        newR.coerceIn(0, 255),
                        newG.coerceIn(0, 255),
                        newB.coerceIn(0, 255)
                    )
                }
            }
        }

        bitmap.setPixels(pixels, 0, BITMAP_SIZE, 0, 0, BITMAP_SIZE, BITMAP_SIZE)
        return bitmap
    }
}
