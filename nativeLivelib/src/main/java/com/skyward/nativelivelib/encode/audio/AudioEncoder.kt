package com.skyward.nativelivelib.encode.audio

import android.media.*
import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.config.AudioConfiguration
import com.skyward.nativelivelib.encode.base.BaseEncode
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.push.RTMPPackage

/**
 * @author skyward
 * @date 2022/1/17 15:56
 * @desc
 *
 **/
class AudioEncoder: BaseEncode {


    private var mLiving = false
    private var mListener: RtmpPacketListener? = null

    private var mediaCodec:MediaCodec? =null
    private var mediaCodecInfo = MediaCodec.BufferInfo()
    private var startTime:Long = 0
    private var presentationTimeUs = System.currentTimeMillis()*1000



    fun initAudioEncoder(audioConfig: AudioConfiguration,minBufferSize:Int){
        val format = MediaFormat.createAudioFormat(audioConfig.mime,audioConfig.sampleRate,audioConfig.channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,audioConfig.aacProfile)
        format.setInteger(MediaFormat.KEY_BIT_RATE,audioConfig.maxBps*1024)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,minBufferSize)
        try {
            mediaCodec = MediaCodec.createEncoderByType(audioConfig.mime)
            mediaCodec?.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()
        }catch (e:Exception){
            e.printStackTrace()
            release()
        }
    }

    override fun encode(data: ByteArray, timestamp: Long) {
        pushAudioDataToMediaCodec(data,timestamp)
        getEncodeData()
    }
   private fun pushAudioDataToMediaCodec(data: ByteArray,timestamp: Long){
        mediaCodec?.let {
            //获取有效的输入缓冲区索引
            val inIndex = it.dequeueInputBuffer(0)
            if(inIndex >= 0){
                val pts = timestamp*1000 -presentationTimeUs
                //根据索引获得输入缓冲区
               it.getInputBuffer(inIndex)?.also { inputBuffer ->
                   //清空以前的旧数据
                    inputBuffer.clear()
                   //填充新数据
                    inputBuffer.put(data,0,data.size)
                   //往编码器塞数据
                   it.queueInputBuffer(inIndex,0,data.size,pts,0)
                }
            }
        }

    }

     fun sendAudioHeader(){
        //没有开始编码音频 先发送空数据头告诉native推流端准备开始音频推流
        val rtmpPackage = RTMPPackage();
//        发音频 让另外一段准备
        val audioDecoderSpecificInfo = byteArrayOf(0x12, 0x08)
        rtmpPackage.buffer = audioDecoderSpecificInfo
        rtmpPackage.type = RTMPPackage.RTMP_PACKET_TYPE_AUDIO_HEAD
        mListener?.addPackage(rtmpPackage)
    }

  private  fun getEncodeData(){
        mediaCodec?.let {
            //获取编码后的缓冲区索引
            var outIndex = it.dequeueOutputBuffer(mediaCodecInfo,0)
            while (outIndex >= 0 && mLiving){
                //初始化接收编码后数据的容器大小
                val outData = ByteArray(mediaCodecInfo.size)
                //获取编码后的数据
                it.getOutputBuffer(outIndex)?.also { outBuffer ->
                    //获取编码后的数据，填充到outData中
                    outBuffer.get(outData)
                    if(startTime == 0L){
                        startTime = mediaCodecInfo.presentationTimeUs/1000
                    }
                    //封装数据到package
                    val rtmpPackage =
                        RTMPPackage(outData, mediaCodecInfo.presentationTimeUs / 1000 - startTime,true)
                    rtmpPackage.type = RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA
                    mListener?.addPackage(rtmpPackage)

                }
                LogUtils.i("myLog ---getEncodeData---outData: ${outData.size}")
                //释放资源
                it.releaseOutputBuffer(outIndex,false)
                //继续取数据，如果没有返回< 0 就跳出循环
                outIndex = it.dequeueOutputBuffer(mediaCodecInfo,0)
            }

        }
    }

    override fun setPresentationTimeUs(presentationTimeUs: Long) {
        this.presentationTimeUs =presentationTimeUs
    }


    override fun setLiving(living: Boolean) {
        this.mLiving = living
    }

    override fun release() {
        mLiving = false
        mediaCodec?.let {
            it.stop()
            it.release()
            mediaCodec =null
        }
        startTime =0

    }

    override fun setEncodeResultListener(listener: RtmpPacketListener) {
        this.mListener = listener
    }



}