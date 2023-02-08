package com.skyward.nativelivelib

import com.blankj.utilcode.util.ToastUtils

class PushLib {

    /**
     * A native method that is implemented by the 'nativelivelib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    private var mListener: PushManagerListener? =null

    companion object {
        // Used to load the 'nativelivelib' library on application startup.
        init {
            System.loadLibrary("pushlivelib")
        }
    }

    /**
     * 初始化推流
     * isMediaCodec：true 硬编 false 软编
     */
    external fun pushInit(isMediaCodec:Boolean)

    /**
     * 设置推流地址,并且开始推流
     */
    external fun startPush(pushUrl:String)

    external fun stopPush()



    /**
     * 传递硬编后的数据到native
     */
    external fun pushMediaCodecEncodeData(data: ByteArray,len:Int,tms:Long,type:Int)

    /**
     * 传输原始视频和音频数据到native去软编
     */
    external fun pushYuvAndPcmData(data: ByteArray,len:Int,type:Int)

    /**
     * 打开视频软编
     */
    external fun initVideoCodec(width:Int,height:Int, fps:Int, bitrate:Int);

    /**
     * 打开音频软编
     */
    external fun initAudioCodec(sampleRate:Int,channel:Int)

    /**
     * 获取样本数量
     */
    external fun getInputSamples():Int


    /**
     * 释放资源
     */
    external fun release()


    /**
     * native层回调java rtmp开始链接
     */
    fun onRtmpConnect(){
        mListener?.onConnect()
    }

    /**
     * native层回调java rtmp链接成功
     */
    fun onRtmpConnectSuccess(){
        mListener?.onConnectSuccess()
    }

    /**
     * native层回调java 推流相关错误回调
     */
    fun onError(errorCode:Int){
        ToastUtils.showShort("发生错误： $errorCode")
    }

    fun setPushManagerListener(listener: PushManagerListener){
        this.mListener = listener
    }


    interface PushManagerListener{
        fun onConnect()
        fun onConnectSuccess()
        fun onError(error:String)
    }
}