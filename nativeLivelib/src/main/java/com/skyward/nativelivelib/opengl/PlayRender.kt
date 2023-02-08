package com.skyward.nativelivelib.opengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author skyward
 * @date 2022/1/27 14:18
 * @desc
 *
 **/
class PlayRender(val context: Context):GLSurfaceView.Renderer {

    //顶点
    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    private val textureData = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )
    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexData.size*4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertexData)
    private var textureBuffer:FloatBuffer = ByteBuffer.allocateDirect(textureData.size*4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(textureData)
    private var program_yuv = 0
    private var avPosition_yuv =0
    private var afPosition_yuv =0
    private var textureId =0
    private var sampler_y =0
    private var sampler_u =0
    private var sampler_v =0
    private var textureId_yuv:IntArray? = null
    private var width_yuv =0
    private var height_yuv =0;
    private var y:ByteBuffer? = null
    private var u:ByteBuffer? =null
    private var v:ByteBuffer? =null

    init {
        vertexBuffer.position(0)
        textureBuffer.position(0)
    }

    fun setYUVRenderData(width: Int,height: Int,y:ByteArray,u:ByteArray,v:ByteArray){
        this.width_yuv = width
        this.height_yuv = height
        this.y = ByteBuffer.wrap(y)
        this.u = ByteBuffer.wrap(u)
        this.v = ByteBuffer.wrap(v)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        initRenderYuv()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f)
        renderYuv()
//        LogUtils.i("myLog --- onDrawFrame----")
        //刷新
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun initRenderYuv(){
        val vertexSource = ShaderUtil.readRawTxt(context, R.raw.vertex_shader)
        val fragmentSource = ShaderUtil.readRawTxt(context,R.raw.fragment_shader)
        program_yuv = ShaderUtil.createProgram(vertexSource, fragmentSource)

        avPosition_yuv = GLES20.glGetAttribLocation(program_yuv,"av_Position")
        afPosition_yuv = GLES20.glGetAttribLocation(program_yuv,"af_Position")

        sampler_y = GLES20.glGetUniformLocation(program_yuv,"sampler_y")
        sampler_u = GLES20.glGetUniformLocation(program_yuv,"sampler_u")
        sampler_v = GLES20.glGetUniformLocation(program_yuv,"sampler_v")

        textureId_yuv = IntArray(3)
        GLES20.glGenTextures(3,textureId_yuv,0)

        for(i in 0..2){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId_yuv!![i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)

        }
    }

    private fun renderYuv(){
        if(width_yuv > 0 && height_yuv > 0 && y != null && u != null && v != null){
            GLES20.glUseProgram(program_yuv)

            GLES20.glEnableVertexAttribArray(avPosition_yuv)
            GLES20.glVertexAttribPointer(avPosition_yuv,2,GLES20.GL_FLOAT,false,8,vertexBuffer)

            GLES20.glEnableVertexAttribArray(afPosition_yuv)
            GLES20.glVertexAttribPointer(afPosition_yuv,2,GLES20.GL_FLOAT,false,8,textureBuffer)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId_yuv!![0])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv,height_yuv,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,y)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId_yuv!![1])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv/2,height_yuv/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,u)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId_yuv!![2])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv/2,height_yuv/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,v)

            GLES20.glUniform1i(sampler_y,0)
            GLES20.glUniform1i(sampler_u,1)
            GLES20.glUniform1i(sampler_v,2)

            y?.clear()
            u?.clear()
            v?.clear()
            y =null
            u=null
            v=null
        }
    }


}