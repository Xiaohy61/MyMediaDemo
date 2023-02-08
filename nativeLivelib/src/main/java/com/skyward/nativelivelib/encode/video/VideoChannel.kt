package com.skyward.nativelivelib.encode.video

import android.content.Context
import android.media.ImageReader
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.skyward.nativelivelib.camera2.AutoFitSurfaceView
import com.skyward.nativelivelib.camera2.Camera2Helper
import com.skyward.nativelivelib.camera2.ICamera2
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.encode.base.BaseChannel
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.push.PushManager
import com.skyward.nativelivelib.push.RTMPPackage
import com.skyward.nativelivelib.utils.ImageUtils
import java.util.concurrent.locks.ReentrantLock

/**
 * @author skyward
 * @date 2022/1/17 22:23
 * @desc
 *
 **/
class VideoChannel(val context: Context, val pushManager: PushManager) : BaseChannel, ImageReader.OnImageAvailableListener,
    RtmpPacketListener {

    private var isLiving = false
    private var mCamera2Helper: Camera2Helper? = null
    private  var mVideoEncoder: VideoEncoder? = null
    private var mNv12: ByteArray? = null
    private var nv21Rotated: ByteArray? = null
    private val reentrantLock = ReentrantLock()
    private var isMediaCodec = true

    fun initVideoChannel(videoConfig: VideoConfiguration, mSurfaceView: AutoFitSurfaceView,isMediaCodec: Boolean) {
        this.isMediaCodec = isMediaCodec
        mCamera2Helper = Camera2Helper(context)
        mCamera2Helper?.setSurfaceView(mSurfaceView, videoConfig.width, videoConfig.height)
        mCamera2Helper?.openCamera(ICamera2.CameraType.BACK)
        mCamera2Helper?.setImageAvailableListener(this)
        if(isMediaCodec){
            mVideoEncoder = VideoEncoder()
            mVideoEncoder?.initVideoEncode(videoConfig)
            mVideoEncoder?.setEncodeResultListener(this)
        }

    }

    fun switchCamera(cameraType: ICamera2.CameraType) {
        mCamera2Helper?.switchCamera(cameraType)
    }



    override fun startLive() {
        isLiving = true
        mVideoEncoder?.setLiving(isLiving)
        mVideoEncoder?.setPresentationTimeUs(System.currentTimeMillis())
    }

   override fun stopLive() {
        isLiving = false
        mVideoEncoder?.setLiving(isLiving)
    }

   override fun release() {
        isLiving = false
        mVideoEncoder?.release()
        mCamera2Helper?.closeCamera()
    }

    override fun addPackage(rtmpPackage: RTMPPackage) {
        pushManager.addPackage(rtmpPackage)
    }


    override fun onImageAvailable(reader: ImageReader?) {
//        LogUtils.i("myLog onImageAvailable: isLiving: $isLiving")
        reader?.let {
            val image = it.acquireNextImage()
            if(!isLiving){
                image.close()
                return@let
            }
            reentrantLock.lock()
            val timestamp = System.currentTimeMillis()
            val planes = image.planes
            val height = it.height
            val stride = planes[0].rowStride



            if (nv21Rotated == null) {
                mNv12 = ByteArray(stride * height * 3 / 2)
                nv21Rotated = ByteArray(stride * height * 3 / 2)
//                LogUtils.i("myLog stride: $stride height: $height")
            }


            val nv21 = ImageUtils.yuv420ToNv21(image)
            ImageUtils.nv21_rotate_to_90(nv21, nv21Rotated, stride, image.height)
            if(isMediaCodec){
                mNv12?.let { nv12 ->
                    //旋转后转nv12 目前硬编码接收的数据必须为NV12
                    val nv12Temp = ImageUtils.nv21toNV12(nv21Rotated, nv12)
                    mVideoEncoder?.encode(nv12Temp,timestamp)
//                    SaveVideoByteFileUtils.writeNv21Bytes(nv12Temp)
                }
            }else{
                val rtmpPackage = RTMPPackage()
                rtmpPackage.isMediaCodec = false
                rtmpPackage.buffer = nv21Rotated!!
                rtmpPackage.type = RTMPPackage.RTMP_PACKET_TYPE_VIDEO
//                LogUtils.i("myLog --- rtmpPackage.buffer: ${rtmpPackage.buffer.size} ")
                addPackage(rtmpPackage)
            }

            reentrantLock.unlock()
            image.close()
        }


    }
}