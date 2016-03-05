
attribute vec4 color;
attribute vec4 position;

varying vec4 fragColor;

void main() {
    vec4 c = vec4(color.r, 1f, 1f, 1.0f);
    fragColor = c;
    gl_PointSize = 20.0f;
    gl_Position = position;
}