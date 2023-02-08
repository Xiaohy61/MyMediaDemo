package com.skyward.nativelivelib

import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.camera2.AutoFitSurfaceView
import com.skyward.nativelivelib.opengl.PlayGLSurfaceView
import com.skyward.nativelivelib.player.PlayVideoListener
import com.skyward.nativelivelib.utils.SaveVideoByteFileUtils

/**
 * @author skyward
 * @date 2022/1/25 19:11
 * @desc
 *
 **/
class PlayLib {
    companion object {
        // Used to load the 'nativelivelib' library on application startup.
        init {
            System.loadLibrary("playerlib")
        }
    }

    private var mPlayListener:PlayVideoListener? = null
    private var mSurfaceView:PlayGLSurfaceView? =null

    fun setPlayListener(listener: PlayVideoListener){
        this.mPlayListener = listener
    }

    fun setGLSurfaceView(surfaceView: PlayGLSurfaceView){
        this.mSurfaceView = surfaceView
    }

    //-----------native回调java方法---------------------//
    fun onCallPrepared(){
       mPlayListener?.onPrepare()
   }

    fun onCallVideoWidthAndHeight(width: Int,height: Int){
        mSurfaceView?.setAspectRatio(width, height)
    }

    //native回调java方法
    fun onCallRenderYUV(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray){
//        LogUtils.i("myLog width: $width height: $height y: ${y.size} u: ${u.size} v: ${v.size}")
        SaveVideoByteFileUtils.writeBytes(y)
        mSurfaceView?.setYuvData(width, height, y, u, v)
    }
    //native回调java方法 播放时间回调
    fun onCallTimeInfo(currentTime:Int,totalTime:Int){

    }

    fun onCallLoad(load:Boolean){

    }
    //-----------native回调java方法---------------------//

    external fun setUrl(url:String)

    external fun startPlay()

    external fun pause()


}