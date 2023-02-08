package com.skyward.nativelivelib.opengl

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception

/**
 * @author skyward
 * @date 2022/1/27 14:29
 * @desc
 *
 **/
object ShaderUtil {

    fun readRawTxt(context:Context,rawId:Int):String{
        val inputStream = context.resources.openRawResource(rawId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuffer()
        var line:String?  =null
        try {
            while (reader.readLine().also { line = it } != null){
                sb.append(line).append("\n")
            }
            reader.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return sb.toString()
    }

    fun loadShader(shaderType:Int,source:String):Int{
        var shader = GLES20.glCreateShader(shaderType)
        if(shader!= 0){
            GLES20.glShaderSource(shader,source)
            GLES20.glCompileShader(shader)
            val compile = IntArray(1)
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compile,0)
            if(compile[0] != GLES20.GL_TRUE){
                GLES20.glDeleteShader(shader)
                shader =0
            }
        }
        return shader
    }

    fun createProgram(vertexSource:String,fragmentSource:String):Int{
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,vertexSource)
        if(vertexShader == 0){
            return 0
        }
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentSource)
        if(fragmentShader == 0){
            return 0
        }
        var program = GLES20.glCreateProgram()
        if(program != 0){
            GLES20.glAttachShader(program,vertexShader)
            GLES20.glAttachShader(program,fragmentShader)
            GLES20.glLinkProgram(program)
            val lineStatus = IntArray(1)
            GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,lineStatus,0)
            if(lineStatus[0] != GLES20.GL_TRUE){
                GLES20.glDeleteProgram(program)
                program =0
            }
        }
        return program
    }

}