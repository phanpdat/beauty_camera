package com.example.beauty_cameracamera_app.filter

data class Filter(
    val id: Int,
    val name: String,
    val params: FilterParams
)


 // BỘ THAM SỐ ĐIỀU KHIỂN MÀU SẮC (THE BRAIN OF SHADER)

data class FilterParams(
    // 1. Độ sáng (Phạm vi: -1.0 đến 1.0, Mặc định: 0.0)
    val brightness: Float = 0f,

    // 2. Độ tương phản (Phạm vi: 0.0 đến 2.0, Mặc định: 1.0)
    val contrast: Float = 1f,

    // 3. Độ rực màu - Vibrance (Giúp màu sắc nổi bật nhưng bảo vệ màu da, Mặc định: 1.0)
    val saturation: Float = 1f, 

    // 4. Các kênh màu dịch chuyển (Dùng để tạo tone màu nóng/lạnh tùy ý)
    val redShift: Float = 0f,   // Tăng/Giảm sắc Đỏ
    val greenShift: Float = 0f, // Tăng/Giảm sắc Xanh lá
    val blueShift: Float = 0f,  // Tăng/Giảm sắc Xanh dương

    // 5. Hệ số Gamma (Điều chỉnh vùng trung tính, giúp ảnh có chiều sâu hơn)
    val gamma: Float = 1f,

    // 6. THÔNG SỐ OVERLAY (PHỦ MÀU CHUYÊN NGHIỆP)
    // Dùng để nhuộm màu cho bức ảnh (Ví dụ: nhuộm hồng cho Filter Pinky)
    val overlayR: Float = 0f,   // Tông Đỏ của lớp phủ
    val overlayG: Float = 0f,   // Tông Xanh lá của lớp phủ 
    val overlayB: Float = 0f,   // Tông Xanh dương của lớp phủ 
    
    // Cường độ trộn màu (0.0 = Không phủ, 1.0 = Phủ hoàn toàn)
    val overlayStrength: Float = 0f
)

object FilterList {
    fun getFilters(): List<Filter> = listOf(
        Filter(0, "Normal", FilterParams()),

        // 1. Daily - Tone sáng hồng tự nhiên (Dùng tone Đỏ nhẹ 1.0f, 0.8f, 0.85f)
        Filter(1, "Daily", FilterParams(
            brightness = 0.05f, contrast = 1.05f, saturation = 1.1f,
            redShift = 0.02f, gamma = 0.95f,
            overlayR = 1.0f, overlayG = 0.8f, overlayB = 0.85f, overlayStrength = 0.1f
        )),

        // 2. Cool - Tone xanh lạnh hiện đại (Dùng tone Xanh dương mạnh 0.7f, 0.8f, 1.0f)
        Filter(2, "Cool", FilterParams(
            brightness = 0.02f, contrast = 1.1f, saturation = 0.9f,
            blueShift = 0.05f, gamma = 1.05f,
            overlayR = 0.7f, overlayG = 0.8f, overlayB = 1.0f, overlayStrength = 0.15f
        )),

        // 3. Warm - Tone nắng chiều ấm áp (Dùng tone Vàng nắng 1.0f, 0.9f, 0.7f)
        Filter(3, "Warm", FilterParams(
            brightness = 0.05f, contrast = 1.05f, saturation = 1.1f,
            redShift = 0.03f, gamma = 0.95f,
            overlayR = 1.0f, overlayG = 0.9f, overlayB = 0.7f, overlayStrength = 0.12f
        )),

        // 4. Movie (Cinematic) - Tone phim chuyên sâu (Dùng màu Teal/Cyan 0.2f, 0.6f, 0.6f)
        Filter(4, "Movie", FilterParams(
            brightness = -0.02f, contrast = 1.2f, saturation = 0.8f,
            gamma = 1.1f,
            overlayR = 0.2f, overlayG = 0.6f, overlayB = 0.6f, overlayStrength = 0.08f
        )),

        // 5. Pinky - Tone hồng kẹo ngọt mơ màng (Dùng hồng đậm 1.0f, 0.5f, 0.7f)
        Filter(5, "Pinky", FilterParams(
            brightness = 0.06f, contrast = 1.0f, saturation = 1.2f,
            gamma = 0.9f,
            overlayR = 1.0f, overlayG = 0.5f, overlayB = 0.7f, overlayStrength = 0.15f
        ))
    )
}
