precision mediump float;

uniform sampler2D textureSampler;

varying vec4 fragColor;
varying vec2 fragTextCoord;

void main() {
    gl_FragColor = fragColor * texture2D(textureSampler, fragTextCoord);
//    vec4 color = fragColor;
//    color.r = 0.0;
//    color.g = fragTextCoord.x;
//    color.b = fragTextCoord.y;
//    gl_FragColor = color;
}