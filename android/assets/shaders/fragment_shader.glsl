#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_paletteTexture;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

void main() {
    vec4 currentPixelColor = texture2D(u_texture, v_texCoords).rgba;
    float pixel = currentPixelColor.r;
    float attr = v_color.g;
    float offset = v_color.b;
    vec2 paletteTextureCoords = vec2(attr + pixel + offset, .5);
    vec3 outputPixelColor = texture2D(u_paletteTexture, paletteTextureCoords).rgb;
    gl_FragColor = vec4(outputPixelColor, currentPixelColor.a);
}
