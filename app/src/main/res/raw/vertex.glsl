uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 a_TextCoordinate;

varying vec2 v_TextCoordinate;

void main(){
    v_TexCoordinate = a_TexCoordinate;
    gl_Position = uMVPMatrix*vPosition;
}