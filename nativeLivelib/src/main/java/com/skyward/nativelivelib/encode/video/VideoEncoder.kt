package com.skyward.nativelivelib.encode.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.encode.base.BaseEncode
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.push.RTMPPackage
import java.nio.ByteBuffer

/**
 * @author skyward
 * @date 2022/1/15 17:28
 * @desc
 *
 **/
class VideoEncoder : BaseEncode {


    private var mediaCodec: MediaCodec? = null
    private var isLiving = false

    // 每一帧编码时间
    private var timeStamp: Long = 0

    // 开始时间
    private var startTime: Long = 0
    private var mListener: RtmpPacketListener? = null
    private var presentationTimeUs = System.currentTimeMillis()*1000
    private  val bufferInfo = MediaCodec.BufferInfo()
    private var rtmpPackage :RTMPPackage? = null



    fun initVideoEncode(videoConfiguration: VideoConfiguration) {
        val format = MediaFormat.createVideoFormat(
            videoConfiguration.mime,
            videoConfiguration.width,
            videoConfiguration.height
        )
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoConfiguration.maxBps * 1024)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoConfiguration.fps)

        //I帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoConfiguration.ifi)
//        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
        format.setInteger(
            MediaFormat.KEY_BITRATE_MODE,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
        )
        format.setInteger(
            MediaFormat.KEY_COMPLEXITY,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
        )
        try {
            mediaCodec = MediaCodec.createEncoderByType(videoConfiguration.mime)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()
        } catch (e: Exception) {
            ToastUtils.showShort("初始化有误：${e.message}")
            e.printStackTrace()
            release()
        }
    }

    override fun setPresentationTimeUs(presentationTimeUs: Long) {
        this.presentationTimeUs = presentationTimeUs*1000
    }


    override fun setLiving(living: Boolean) {
        this.isLiving = living
    }


    override fun encode(data: ByteArray, timestamp: Long) {
        if (mediaCodec == null) {
            return
        }
        mediaCodec?.let { codec ->
            if (isLiving) {
                //同步编码
                try {
                    pushDataToMediaCodec(data, codec,timestamp)
                    getEncodeData(codec)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }else{
                mediaCodec?.stop()
            }
        }
    }

    /**
     * 推数据到编码器(即塞数据到dps芯片）
     */
    private fun pushDataToMediaCodec(data: ByteArray, codec: MediaCodec,timestamp: Long) {
        //获取编码输入缓冲区索引
        val inIndex = codec.dequeueInputBuffer(0)
        if (inIndex >= 0) {
            val pts = timestamp*1000 -presentationTimeUs
            //根据获得的索引获取输入缓冲区
            val byteBuffer = codec.getInputBuffer(inIndex)
            byteBuffer?.let { inputBuffer ->
                //清空以前的旧数据
                inputBuffer.clear()
                //填充新数据
                inputBuffer.put(data, 0, data.size)
                //通知编码
                codec.queueInputBuffer(inIndex, 0, data.size, pts, 0)
            }
        }
    }

    /**
     * 从编码器中获取编码后的数据
     */
    private fun getEncodeData(codec: MediaCodec) {

        if (System.currentTimeMillis() - timeStamp >= 2000) {
            val params = Bundle()
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            //  dsp 芯片触发I帧
            codec.setParameters(params)
            timeStamp = System.currentTimeMillis()
        }
        //获取编码输入缓冲区索引
        var outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        while (outIndex >= 0 && isLiving) {
            if (startTime == 0L) {
                startTime = bufferInfo.presentationTimeUs / 1000
            }
            //获取编码输入缓冲区
            val buffer = codec.getOutputBuffer(outIndex) as ByteBuffer
//            LogUtils.i("myLog bufferInfo.size: ${bufferInfo.size} buffer.remaining():  ${buffer.remaining()}")
            val outData = ByteArray(bufferInfo.size)
            //缓冲区数据输出到outData
            buffer.get(outData, 0, outData.size)

            //封装数据到package

            if(rtmpPackage == null){
                rtmpPackage = RTMPPackage()
            }
            rtmpPackage?.let { pack ->
                pack.isMediaCodec = true
                pack.buffer = outData
                pack.tms = bufferInfo.presentationTimeUs / 1000 - startTime
                pack.type = RTMPPackage.RTMP_PACKET_TYPE_VIDEO
//                LogUtils.i("myLog --- rtmpPackage.buffer: ${rtmpPackage.buffer.size} ")
                mListener?.addPackage(pack)
            }

//            SaveVideoByteFileUtils.writeContent(outData)
//            SaveVideoByteFileUtils.writeBytes(outData)

//            LogUtils.i("myLog outData: ${outData.size}")
            codec.releaseOutputBuffer(outIndex, false)
            outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }


    override fun release() {
        mediaCodec?.let {
            isLiving = false
            it.stop()
            it.release()
            mediaCodec = null

        }
    }


    override fun setEncodeResultListener(listener: RtmpPacketListener) {
        this.mListener = listener
    }
}