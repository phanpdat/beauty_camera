attribute vec4 aPosition;
attribute vec4 aTexCoord;
varying vec2 vTexCoord;
uniform mat4 uSTMatrix; // Ma trận biến đổi từ SurfaceTexture

void main() {
    gl_Position = aPosition;
    vTexCoord = (uSTMatrix * aTexCoord).xy;
}
