
uniform mat4 mvp;
uniform vec2 touch;

attribute vec4 color;
attribute vec4 position;
attribute vec2 textCoord;

varying vec4 fragColor;
varying vec2 fragTextCoord;

void main() {
    fragColor = color;
    fragTextCoord = textCoord;

    float d = distance(touch, vec2(position.x, position.y));

    if(d < 0.1f){
        fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
    }

    gl_PointSize = 20.0f;
    gl_Position = mvp * position;
}