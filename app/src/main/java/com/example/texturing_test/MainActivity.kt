package com.example.texturing_test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {

    private lateinit var gLView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gLView = MyGLSurfaceView(this)
        setContentView(gLView)
    }
}

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)
    }
}

class MyGLRenderer( context : Context) : GLSurfaceView.Renderer {

    private val mContext : Context = context
    private var vPMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)

    private lateinit var mRectangle: Rectangle

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        mRectangle = Rectangle(mContext)
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix,0,0f,0f,-3f,0f,0f,0f,0f,1.0f,0.0f)

        Matrix.multiplyMM(vPMatrix,0,projectionMatrix,0,viewMatrix,0)

        mRectangle.draw(vPMatrix)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio:Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix,0,-ratio,ratio,-1f,1f,2f,10f)

    }
}

// number of coordinates per vertex in this array
const val COORDS_PER_VERTEX = 3


fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    return GLES20.glCreateShader(type).also { shader ->

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
    }
}

class Rectangle(context:Context) {
    private val myContext: Context = context
    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())

    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode = fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    private var vPMatrixHandle: Int = 0

    private var mProgram: Int
    val colorR = floatArrayOf(1.0f, 0f, 0f, 1f)
    val colorG = floatArrayOf(0f, 1.0f, 0f, 1f)
    val colorB = floatArrayOf(0f, 0f, 1.0f, 1f)

    private var rectangleCoords = floatArrayOf(     // in counterclockwise order:
            -0.5f, 0.5f, 0.0f,      // top left
            0.5f, 0.5f, 0.0f,       // top right
            -0.5f, -0.5f, 0.0f,    // bottom left
            -0.5f, -0.5f, 0.0f,    // bottom left
            0.5f, -0.5f, 0.0f,      // bottom right
            0.5f, 0.5f, 0.0f       // top right
    )

    private var texCoord = floatArrayOf(
            // 위의 버텍스 순서와 맞춰줘야함.
            // //만일 클리핑 텍스쳐링 하고싶으면 그 비율만큼의 수치를 적어주면 됨
            0.0f, 1.0f, //top left
            1.0f, 1.0f, //top right
            0.0f, 0.0f, // bottom left
            0.0f, 0.0f, //bottom left
            1.0f, 0.0f, //bottom right
            1.0f, 1.0f //top right
    )
    private var vertexBuffer: FloatBuffer =
            ByteBuffer.allocateDirect(rectangleCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(rectangleCoords)
                    position(0)
                }
            }
    private var texBuffer = ByteBuffer.allocateDirect(texCoord.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            for (texture in texCoord) {
                put(texture)
            }
            position(0)
        }
    }

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }


    private val vertexCount: Int = 18 / COORDS_PER_VERTEX
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private var mTextureUniformHandle = 0
    private var mTextureCoordinateHandle = 0
    private var mTextureCoordinateDatasize = 2
    private var mTextureDataHandle = 0

    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(mColorHandle, 1, colorR, 0)

        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)


        //텍셀관련 language, 셰이더파일 코드랑 연관돼있음
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDatasize, GLES20.GL_FLOAT, false, mTextureCoordinateDatasize * 4, texBuffer)

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")
        mTextureDataHandle = loadTexture(myContext, R.drawable.img)

        GLES20.glActiveTexture((GLES20.GL_TEXTURE0))
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)
        GLES20.glUniform1i(mTextureUniformHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)

    }
}


    fun loadTexture(context:Context, resourceId:Int):Int{
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1,textureHandle,0)
        if(textureHandle[0]!=0){
            val options = BitmapFactory.Options()
            options.inScaled =false
            // 리소스 읽기
            val bitmap = BitmapFactory.decodeResource(context.resources,resourceId,options)
            //옵션? : 이미지 크기에 따라 스케일링을 원래 다 해줌.
            // 지금은 우리가 텍스쳐를 지정해주었기때문에 옵션 끈 상태(위의 false)


            // 오픈지엘에 텍스쳐 바인드
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureHandle[0])

            //필터링세팅
            //픽셀 늘렸을때, 줄였을때의 기법 어떻게 하는지. 세번째 파라미터로 종류 결정가능 (지금은 nearest filtering)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST)

            // 텍스쳐와 관련된 유용한 기능. 밉맵레벨 및 가장자리 등의 설정이 가능함
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)

            //이 비트값을 다른오브젝트에 또 쓸것이라고 선언하는 코드로, 선언해주면 메모리에 쭉 남아있음..
            bitmap.recycle()

        }
        if(textureHandle[0]==0){ // 텍스쳐 할당이 안 된 경우
            throw RuntimeException("Error loading texture")
        }
        return textureHandle[0]
    }


