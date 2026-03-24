#extension GL_OES_EGL_image_external : require
precision highp float;

varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;
uniform float uIntensity; // Level of beauty (0.0 to 1.0)

// Thông số làm mịn
const float step_width = 1.0 / 720.0;
const float step_height = 1.0 / 1280.0;

void main() {
    vec4 centralColor = texture2D(uTexture, vTexCoord);
    
    // Nếu intensity = 0, trả về ảnh gốc luôn cho nhanh
    if (uIntensity <= 0.01) {
        gl_FragColor = centralColor;
        return;
    }

    // Thuật toán làm mịn da đơn giản: Blur chọn lọc
    // Lấy mẫu các pixel xung quanh để tính toán độ mịn
    vec2 offsets[8];
    offsets[0] = vec2(-step_width, -step_height);
    offsets[1] = vec2(0.0, -step_height);
    offsets[2] = vec2(step_width, -step_height);
    offsets[3] = vec2(-step_width, 0.0);
    offsets[4] = vec2(step_width, 0.0);
    offsets[5] = vec2(-step_width, step_height);
    offsets[6] = vec2(0.0, step_height);
    offsets[7] = vec2(step_width, step_height);

    float sampleColor = centralColor.g * 2.0;
    for(int i = 0; i < 8; i++) {
        sampleColor += texture2D(uTexture, vTexCoord + offsets[i]).g;
    }
    
    sampleColor /= 10.0;
    
    // Tính toán độ tương phản để giữ lại chi tiết (mắt, môi)
    float highPass = centralColor.g - sampleColor + 0.5;
    
    // Áp dụng hard light blending
    for(int i = 0; i < 5; i++) {
        if(highPass <= 0.5) {
            highPass = highPass * highPass * 2.0;
        } else {
            highPass = 1.0 - ((1.0 - highPass) * (1.0 - highPass) * 2.0);
        }
    }
    
    // Tính toán độ sáng để làm trắng da nhẹ
    float lumance = dot(centralColor.rgb, vec3(0.299, 0.587, 0.114));
    float alpha = pow(lumance, 0.5); // Điều chỉnh độ cong của làm mịn
    
    vec3 smoothColor = mix(centralColor.rgb, vec3(highPass), alpha * uIntensity);
    
    // Làm trắng nhẹ (Brightening)
    smoothColor = mix(smoothColor, centralColor.rgb + vec3(0.05 * uIntensity), 0.5);

    gl_FragColor = vec4(smoothColor, 1.0);
}
