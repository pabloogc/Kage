precision mediump float;

uniform sampler2D textureSampler;

varying float fragWhite;
varying vec4 fragColor;
varying vec2 fragTextCoord;

void main() {
    vec4 t = fragColor * texture2D(textureSampler, fragTextCoord);
    gl_FragColor = mix(vec4(1.0, 1.0, 1.0, 1.0), t, fragWhite);
//    gl_FragColor = t;
//    gl_FragColor = fragColor;
//    if(gl_FragColor.r == 0.0f) gl_FragColor.r = 1.0f;
//    vec4 color = fragColor;
//    color.r = 1.0;
//    color.g = fragTextCoord.s;
//    color.b = fragTextCoord.t;
//    gl_FragColor = color;
}