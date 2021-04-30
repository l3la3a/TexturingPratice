precision mediump float;
uniform vec4 vColor;
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate; //텍셀값 받기

void main(){
    gl_FragColor = vColor*texture2D(u_Texture,v_TexCoordinate); //텍셀값을 받아 컬러값으로 계산, 최종 frag컬러값이 나옴
}