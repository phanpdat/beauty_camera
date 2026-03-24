#extension GL_OES_EGL_image_external : require
precision highp float;

varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;
uniform sampler2D uLutTexture;
uniform float uIntensity;      // LUT filter intensity (0.0 to 1.0)
uniform float uBeautyLevel;    // Skin smoothing level (0.0 to 1.0)

// Thông số pixel (cho Beauty)
const float step_width = 1.0 / 720.0;
const float step_height = 1.0 / 1280.0;

// ----- Beauty Smoothing (Bilateral-like) -----
vec3 getBeauty(vec4 color) {
    if (uBeautyLevel <= 0.05) return color.rgb;
    
    // Blur nhẹ vùng green channel (độ sâu màu da)
    float sampleColor = color.g * 2.0;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(-step_width, -step_height)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(0.0, -step_height)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(step_width, -step_height)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(-step_width, 0.0)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(step_width, 0.0)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(-step_width, step_height)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(0.0, step_height)).g;
    sampleColor += texture2D(uTexture, vTexCoord + vec2(step_width, step_height)).g;
    sampleColor /= 10.0;
    
    // High-pass để bảo vệ các góc cạnh sẫm màu (mắt, mày)
    float highPass = color.g - sampleColor + 0.5;
    for(int i = 0; i < 3; i++) {
        highPass = (highPass < 0.5) ? (highPass * highPass * 2.0) : (1.0 - (1.0 - highPass) * (1.0 - highPass) * 2.0);
    }
    
    // Lumance-based alpha: Chỉ làm mịn ở vùng sáng (thường là da)
    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float alpha = pow(luminance, 0.5);
    
    return mix(color.rgb, vec3(highPass), alpha * uBeautyLevel);
}

// ----- LUT Look-up -----
vec4 lookupLUT(vec3 color) {
    float blueColor = color.b * 63.0;

    vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);

    vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);
    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);

    vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);
    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);

    vec4 newColor1 = texture2D(uLutTexture, texPos1);
    vec4 newColor2 = texture2D(uLutTexture, texPos2);

    return mix(newColor1, newColor2, fract(blueColor));
}

// ----- Skin Protection -----
float detectSkin(vec3 color) {
    float r = color.r; float g = color.g; float b = color.b;
    return (r > 0.3725 && g > 0.1568 && b > 0.0784 && r > g && r > b && (r - g) > 0.0588) ? 1.0 : 0.0;
}

void main() {
    // 1. Phôi màu gốc
    vec4 original = texture2D(uTexture, vTexCoord);
    
    // 2. Làm mịn da (Beauty)
    vec3 beautyResult = getBeauty(original);
    
    // 3. Nhận diện vùng da
    float skinMask = detectSkin(beautyResult);
    
    // 4. Chỉnh màu (LUT)
    vec4 lutResult = lookupLUT(beautyResult);
    
    // 5. Blend thông minh: Bảo vệ da khỏi bị ám màu quá nặng
    // Nếu là vùng da (skinMask = 1.0), giảm nhẹ cường độ filter 
    float finalIntensity = mix(uIntensity, uIntensity * 0.7, skinMask);
    
    gl_FragColor = mix(vec4(beautyResult, 1.0), lutResult, finalIntensity);
}
