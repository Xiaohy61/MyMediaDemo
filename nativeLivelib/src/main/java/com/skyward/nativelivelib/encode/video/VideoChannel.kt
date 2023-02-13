package com.skyward.nativelivelib.encode.video

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import android.view.WindowManager
import com.blankj.utilcode.util.LogUtils

import com.skyward.nativelivelib.camera2.AutoFitSurfaceView
import com.skyward.nativelivelib.camera2.Camera2Helper
import com.skyward.nativelivelib.camera2.ICamera2
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.encode.base.BaseChannel
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.ext.getCameraRotation

import com.skyward.nativelivelib.push.PushManager
import com.skyward.nativelivelib.push.RTMPPackage
import com.skyward.nativelivelib.utils.ConvertUtils
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
    private val reentrantLock = ReentrantLock()
    private var isMediaCodec = true



    fun initVideoChannel(videoConfig: VideoConfiguration, mSurfaceView: AutoFitSurfaceView, isMediaCodec: Boolean) {
        this.isMediaCodec = isMediaCodec
        mCamera2Helper = Camera2Helper(context)
//
        mCamera2Helper?.setSurfaceView(mSurfaceView, videoConfig.width, videoConfig.height)
        mCamera2Helper?.openCamera(ICamera2.CameraType.BACK)


        //这个帧率是控制视频编码的真正帧率
        mCamera2Helper?.setPreviewFps(videoConfig.fps)
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
        reader?.let {
            val image = it.acquireNextImage()

            if(!isLiving){
                image.close()
                return@let
            }
            reentrantLock.lock()
            val timestamp = System.currentTimeMillis()


            if(isMediaCodec){
               val nv12 =  ConvertUtils.YUV_420_888toNV12(image,context.getCameraRotation())
                mVideoEncoder?.encode(nv12,timestamp)
            }else{
                val i420 = ConvertUtils.YUV_420_888toI420(image,context.getCameraRotation())
                var rtmpPackage :RTMPPackage? = null
                if(rtmpPackage == null){
                    rtmpPackage = RTMPPackage()
                }
                rtmpPackage.let { pack ->
                    pack.isMediaCodec = false
                    pack.buffer = i420
                    pack.type = RTMPPackage.RTMP_PACKET_TYPE_VIDEO
//                LogUtils.i("myLog --- rtmpPackage.buffer: ${rtmpPackage.buffer.size} ")
                    addPackage(pack)
                }
            }

            reentrantLock.unlock()
            image.close()
        }


    }



}