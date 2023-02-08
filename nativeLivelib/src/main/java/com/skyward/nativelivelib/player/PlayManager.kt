package com.skyward.nativelivelib.player

import com.skyward.nativelivelib.PlayLib
import com.skyward.nativelivelib.opengl.PlayGLSurfaceView

/**
 * @author skyward
 * @date 2022/1/25 19:11
 * @desc
 *
 **/
class PlayManager {

    private val playLib = PlayLib()


    fun setUrl(url:String){
        playLib.setUrl(url)
    }

    fun startPlay(){
        playLib.startPlay()
    }

    fun pause(){
        playLib.pause()
    }

    fun setGLSurfaceView(surfaceView: PlayGLSurfaceView){
        playLib.setGLSurfaceView(surfaceView)
    }

    fun setPlayListener(listener: PlayVideoListener){
        playLib.setPlayListener(listener)
    }
}