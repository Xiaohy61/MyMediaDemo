package com.skyward.nativelivelib.push

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.skyward.nativelivelib.PushLib
import com.skyward.nativelivelib.camera2.AutoFitSurfaceView
import com.skyward.nativelivelib.config.AudioConfiguration
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.encode.audio.AudioChannel
import com.skyward.nativelivelib.encode.video.VideoChannel
import java.util.concurrent.LinkedBlockingDeque

/**
 * @author skyward
 * @date 2022/1/15 21:36
 * @desc
 *
 **/
class PushManager(val context: Context) : Handler.Callback, RtmpPacketListener, PushLib.PushManagerListener {

    private val mPushRtmpHandlerThread = HandlerThread("PushRtmpHandlerThread").apply { start() }
    private val mPushRtmpHandler = Handler(mPushRtmpHandlerThread.looper, this)

    private val queue = LinkedBlockingDeque<RTMPPackage>()
    private var isLiving = false
    private val nativeLib = PushLib()
    private var mVideoConfiguration: VideoConfiguration? = null
    private var mAudioConfiguration:AudioConfiguration? = null
    private val PRE_PUSH= 100
    private val START_PUSH = 101
    private val STOP_PUSH =102

    private val PUSH_ENCODE_DATA = 1001
    private val PUSH_YUV_PCM_DATA = 1002
    private lateinit var mVideoChannel: VideoChannel
    private lateinit var mAudioChannel: AudioChannel
    private var mUrl =""

    private val mPushControlHandler = Handler(Looper.getMainLooper(),
        Handler.Callback {
            when(it.what){
                PRE_PUSH ->{
                    nativeLib.startPush(mUrl)
                }
                START_PUSH -> {
                    LogUtils.i("myLog handleMessage START_PUSH isLiving: $isLiving ${it.what}")
                    isLiving = true
                    mVideoChannel.startLive()
                    mAudioChannel.startLive()
                }
                STOP_PUSH ->{
                    isLiving = false
                    LogUtils.i("myLog handleMessage STOP_PUSH isLiving: $isLiving ${it.what}")
                    mVideoChannel.stopLive()
                    mAudioChannel.stopLive()
                    nativeLib.stopPush()
                }
            }
            false
        })



    fun config(
        videoConfiguration: VideoConfiguration,
        audioConfiguration: AudioConfiguration,
        autoFitSurfaceView: AutoFitSurfaceView,
        isMediaCodec:Boolean
    ) {
        nativeLib.pushInit(isMediaCodec)
        nativeLib.setPushManagerListener(this)
        //走软编，初始化软编相关配置信息
        if(!isMediaCodec){
            nativeLib.initVideoCodec(videoConfiguration.width,videoConfiguration.height,
                videoConfiguration.fps,videoConfiguration.maxBps)
            nativeLib.initAudioCodec(audioConfiguration.sampleRate,audioConfiguration.channelCount)
        }

        this.mVideoConfiguration = videoConfiguration
        this.mAudioConfiguration = audioConfiguration
        mVideoChannel = VideoChannel(context, this)
        mVideoChannel.initVideoChannel(videoConfiguration, autoFitSurfaceView,isMediaCodec)
        mAudioChannel = AudioChannel(this)
        mAudioChannel.initAudioChannel(audioConfiguration,isMediaCodec)


    }

    fun getAudioInputByteNum():Int{
       return nativeLib.getInputSamples()*2
    }

    fun startLive(url: String) {
        mUrl = url
        mPushControlHandler.obtainMessage(PRE_PUSH).sendToTarget()
    }


    fun stopLive() {
        mPushControlHandler.obtainMessage(STOP_PUSH).sendToTarget()
    }


    fun release() {
        isLiving = false
        nativeLib.release()
        mAudioChannel.release()
        mVideoChannel.release()
        mPushRtmpHandler.removeCallbacksAndMessages(null)
        mPushControlHandler.removeCallbacksAndMessages(null)
        mPushRtmpHandlerThread.quit()
    }


    override fun addPackage(rtmpPackage: RTMPPackage) {
        if (!isLiving) {
//            LogUtils.i("myLog  addPackage isLiving: $isLiving")
            return
        }
//        LogUtils.i("myLog addPackage---： ${queue.size} isLiving: $isLiving")
        queue.add(rtmpPackage)
        if(rtmpPackage.isMediaCodec){
            mPushRtmpHandler.obtainMessage(PUSH_ENCODE_DATA).sendToTarget()
//            LogUtils.i("myLog --- PUSH_ENCODE_DATA---")
        }else{
            mPushRtmpHandler.obtainMessage(PUSH_YUV_PCM_DATA).sendToTarget()
//            LogUtils.i("myLog --- PUSH_YUV_PCM_DATA---")
        }

    }

    private fun pushEncodeData() {
//        LogUtils.i("myLog pushEncodeData isLiving: $isLiving")
        while (isLiving) {
            try {
                val rtmpPackage = queue.take()
//                LogUtils.i("myLog 推流啦 rtmpPackage----- rtmpPackage： ${queue.size}")
                if (rtmpPackage.buffer.isNotEmpty()) {
                    nativeLib.pushMediaCodecEncodeData(
                        rtmpPackage.buffer,
                        rtmpPackage.buffer.size,
                        rtmpPackage.tms,
                        rtmpPackage.type
                    )
                }

            } catch (e: Exception) {
                LogUtils.i("myLog e : ${e.message}")
                e.printStackTrace()
            }
        }


    }

    private fun pushYuvAndPcmData(){
        while (isLiving) {
            try {
                val rtmpPackage = queue.take()
//                LogUtils.i("myLog 推流啦 rtmpPackage----- rtmpPackage： ${queue.size}")
                if (rtmpPackage.buffer.isNotEmpty()) {
                    nativeLib.pushYuvAndPcmData(
                        rtmpPackage.buffer,
                        rtmpPackage.buffer.size,
                        rtmpPackage.type
                    )
                }

            } catch (e: Exception) {
                LogUtils.i("myLog e : ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun handleMessage(msg: Message): Boolean {
//        LogUtils.i("myLog handleMessage msg.what: ${msg.what} id: ${Thread.currentThread().name}")
        when (msg.what) {
            PUSH_ENCODE_DATA -> {
                pushEncodeData()
            }
            PUSH_YUV_PCM_DATA ->{
                pushYuvAndPcmData()
            }
        }
        return false
    }

    override fun onConnect() {
        ToastUtils.showShort("开始链接")
    }

    override fun onConnectSuccess() {
        ToastUtils.showShort("链接成功")
        mPushControlHandler.obtainMessage(START_PUSH).sendToTarget()
    }

    override fun onError(error: String) {
        ToastUtils.showShort("链接失败 error: $error")
    }

}