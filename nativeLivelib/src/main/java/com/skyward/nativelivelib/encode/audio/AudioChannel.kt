package com.skyward.nativelivelib.encode.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.config.AudioConfiguration
import com.skyward.nativelivelib.encode.base.BaseChannel
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.push.PushManager
import com.skyward.nativelivelib.push.RTMPPackage
import com.skyward.nativelivelib.utils.SaveVideoByteFileUtils

/**
 * @author skyward
 * @date 2022/1/17 15:56
 * @desc
 *
 **/
class AudioChannel(val pushManager: PushManager): BaseChannel, RtmpPacketListener,Handler.Callback {

    private val mAudioChannelThread = HandlerThread("AudioChannelThread").apply { start() }
    private val mAudioChannelHandler = Handler(mAudioChannelThread.looper, this)
    private  var mAudioEncoder: AudioEncoder? = null
    private  var mAudioRecord: AudioRecord? =null
    private var isLiving = false
    private var minBufferSize =0
    private val START_LIVE = 100
    private val STOP_LIVE =101
    private val ENCODING = 102
    private var isMediaCodec = true
    private var rtmpPackage :RTMPPackage? = null
    private val mAudioControlHandler = Handler(
        Looper.myLooper()!!,
        Handler.Callback {
            when(it.what){
                START_LIVE ->{
                    isLiving = true
                    //先发送音频头信息
                    mAudioEncoder?.sendAudioHeader()
                    mAudioChannelHandler.obtainMessage(ENCODING).sendToTarget()
                }
                STOP_LIVE ->{
                    isLiving = false
                    mAudioEncoder?.setLiving(isLiving)
                }
            }
            false
        })



    @SuppressLint("MissingPermission")
    fun initAudioChannel(audioConfig: AudioConfiguration,isMediaCodec: Boolean){
        this.isMediaCodec = isMediaCodec
        minBufferSize = getMinBufferSize(audioConfig)
        LogUtils.i("myLog initAudioChannel: minBufferSize: $minBufferSize")
        if(isMediaCodec){
            mAudioEncoder = AudioEncoder()
            mAudioEncoder?.initAudioEncoder(audioConfig,minBufferSize)
            mAudioEncoder?.setEncodeResultListener(this)
        }else{
            minBufferSize *= 2
        }


        var channelConfig = AudioFormat.CHANNEL_IN_STEREO
        //单声道
        if(audioConfig.channelCount ==1){
            channelConfig = AudioFormat.CHANNEL_IN_MONO
        }
        minBufferSize =  if(pushManager.getAudioInputByteNum() > minBufferSize) pushManager.getAudioInputByteNum() else minBufferSize
//        LogUtils.i("myLog initAudioChannel: audioConfig.sampleRate: ${audioConfig.sampleRate} channelConfig: $channelConfig" +
//                " minBufferSize: $minBufferSize audioConfig.audioFormat: ${audioConfig.audioFormat}")
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,audioConfig.sampleRate,channelConfig,
        audioConfig.audioFormat,minBufferSize)
    }



    override fun startLive(){

        mAudioControlHandler.obtainMessage(START_LIVE).sendToTarget()
    }


   override fun stopLive(){
       mAudioControlHandler.obtainMessage(STOP_LIVE).sendToTarget()
    }

   override fun release(){
        isLiving = false
        mAudioRecord?.apply {
            stop()
            release()
            mAudioRecord = null
        }
        mAudioEncoder?.release()
    }


    override fun addPackage(rtmpPackage: RTMPPackage) {
        pushManager.addPackage(rtmpPackage)
    }

    override fun handleMessage(msg: Message): Boolean {
        when(msg.what){
            ENCODING ->{
                mAudioEncoder?.setLiving(isLiving)
                mAudioRecord?.startRecording()

               val buffer = if(pushManager.getAudioInputByteNum() > 0){
                    ByteArray(pushManager.getAudioInputByteNum())
                }else{
                    ByteArray(minBufferSize)
                }
//                LogUtils.i("myLog buffer: ${buffer.size} minBufferSize: $minBufferSize")
                if(isMediaCodec){
//                    //先发送音频头信息
//                    mAudioEncoder?.sendAudioHeader()
                    mAudioEncoder?.setPresentationTimeUs(System.currentTimeMillis())
                }
                while (isLiving){
                    //从麦克风中读取pcm数据放到buffer容器中
                    val len = mAudioRecord?.read(buffer,0,buffer.size)!!

                    if(len <= 0){
                        continue
                    }
//                        SaveVideoByteFileUtils.savePcm(buffer)
                    if(isMediaCodec){
                        //送去硬编码
                        val timestamp = System.currentTimeMillis()
                        mAudioEncoder?.encode(buffer,timestamp)

//                        LogUtils.i("myLog buffer size: ${buffer.size}")
                    }else{
//                        SaveVideoByteFileUtils.savePcm(buffer)
                        //软编pcm数据回调
                        if(rtmpPackage == null){
                            rtmpPackage = RTMPPackage()
                        }
                        rtmpPackage?.let { pack ->
                            pack.isMediaCodec = false
                            pack.buffer = buffer
                            pack.type = RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA
//                LogUtils.i("myLog --- rtmpPackage.buffer: ${rtmpPackage.buffer.size} ")
                            addPackage(pack)
                        }
                    }

                }
            }

        }
        return false
    }

   private fun getMinBufferSize(audioConfig: AudioConfiguration):Int{
        var channelConfig = AudioFormat.CHANNEL_IN_STEREO
        //单声道
        if(audioConfig.channelCount ==1){
            channelConfig = AudioFormat.CHANNEL_IN_MONO
        }
//       LogUtils.i("myLog getMinBufferSize channelConfig: $channelConfig audioConfig.audioFormat: ${audioConfig.audioFormat}")
        return AudioRecord.getMinBufferSize(audioConfig.sampleRate,channelConfig,audioConfig.audioFormat)
    }
}