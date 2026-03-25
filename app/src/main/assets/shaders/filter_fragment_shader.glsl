#extension GL_OES_EGL_image_external : require
precision highp float;

varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;

// PARAMS (Hệ thống màu chuẩn)
uniform float uBrightness;
uniform float uContrast;
uniform float uSaturation;
uniform float uRedShift;
uniform float uGreenShift;
uniform float uBlueShift;
uniform float uGamma;
uniform float uIntensity;

// OVERLAY (Màu sắc)
uniform vec3 uOverlayColor;
uniform float uOverlayStrength;

vec3 applyVibrance(vec3 color, float vibrance) {
    float average = (color.r + color.g + color.b) / 3.0;
    float mx = max(color.r, max(color.g, color.b));
    float amt = (mx - average) * (-vibrance * 3.0);
    return mix(color, vec3(mx), amt);
}

void main() {
    // 1. SIÊU LÀM NÉT (Ultra-Sharpen Algorithm) - Cải thiện độ trong trẻo 100%
    float offset = 1.0 / 1024.0; // Sử dụng lưới lấy mẫu siêu mịn
    vec3 baseColor = texture2D(uTexture, vTexCoord).rgb;
    
    // Kỹ thuật Laplacian Sharpening: Lấy mẫu 5 điểm quanh tâm
    vec3 right  = texture2D(uTexture, vTexCoord + vec2(offset, 0.0)).rgb;
    vec3 left   = texture2D(uTexture, vTexCoord + vec2(-offset, 0.0)).rgb;
    vec3 top    = texture2D(uTexture, vTexCoord + vec2(0.0, -offset)).rgb;
    vec3 bottom = texture2D(uTexture, vTexCoord + vec2(0.0, offset)).rgb;
    
    // Thuật toán: Làm nổi bật sự khác biệt của trung tâm so với vùng lân cận
    vec3 sharpColor = baseColor * 5.0 - (left + right + top + bottom);
    vec3 result = mix(baseColor, sharpColor, 0.65); // Tăng mức độ nét gấp đôi!
    
    vec3 original = result;

    // 2. BRIGHTNESS & CONTRAST
    result = (result - 0.5) * uContrast + 0.5 + uBrightness;

    // 3. VIBRANCE (Bảo vệ màu da)
    result = applyVibrance(result, uSaturation - 1.0);

    // 4. COLOR SHIFT & OVERLAY
    result.r += uRedShift;
    result.g += uGreenShift;
    result.b += uBlueShift;
    if (uOverlayStrength > 0.0) {
        result = mix(result, uOverlayColor, uOverlayStrength);
    }

    // 5. GAMMA CORRECTION
    result = pow(max(result, 0.0), vec3(uGamma));

    // 6. BEAUTY CURVE (Mềm mại S-Curve)
    vec3 sCurve = result * result * (3.0 - 2.0 * result);
    result = mix(result, sCurve, 0.4); 

    // 7. VIGNETTE (Tập trung trung tâm)
    float dist = distance(vTexCoord, vec2(0.5, 0.5));
    float vignette = smoothstep(0.8, 0.4, dist);
    result = mix(result, result * vignette, 0.25);

    gl_FragColor = vec4(mix(original, result, uIntensity), 1.0);
}
