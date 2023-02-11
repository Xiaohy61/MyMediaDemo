package com.skyward.nativelivelib.encode.video

import android.content.Context
import android.media.ImageReader
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.camera2.Camera2Helper
import com.skyward.nativelivelib.camera2.ICamera2
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.encode.base.BaseChannel
import com.skyward.nativelivelib.encode.base.RtmpPacketListener
import com.skyward.nativelivelib.push.PushManager
import com.skyward.nativelivelib.push.RTMPPackage
import com.skyward.nativelivelib.utils.ConvertUtils
import com.skyward.nativelivelib.utils.ImageUtils
import com.skyward.nativelivelib.utils.SaveVideoByteFileUtils
import me.xcyoung.opengl.camera.widget.camera.CameraXController
import java.util.concurrent.locks.ReentrantLock

/**
 * @author skyward
 * @date 2022/1/17 22:23
 * @desc
 *
 **/
class VideoChannel(val context: Context, val pushManager: PushManager) : BaseChannel, ImageReader.OnImageAvailableListener,
     ImageAnalysis.Analyzer,
    RtmpPacketListener {

    private var isLiving = false
    private var mCamera2Helper: Camera2Helper? = null
    private  var mVideoEncoder: VideoEncoder? = null
    private var mNv12: ByteArray? = null
    private var nv21Rotated: ByteArray? = null
    private val reentrantLock = ReentrantLock()
    private var isMediaCodec = true
    private val cameraXController = CameraXController()

    fun initVideoChannel(videoConfig: VideoConfiguration, mSurfaceProvider: Preview.SurfaceProvider, isMediaCodec: Boolean) {
        this.isMediaCodec = isMediaCodec
//        mCamera2Helper = Camera2Helper(context)
//
//        mCamera2Helper?.setSurfaceView(mSurfaceView, videoConfig.width, videoConfig.height)
//        mCamera2Helper?.openCamera(ICamera2.CameraType.BACK)
//
//
//        //这个帧率是控制视频编码的真正帧率
//        mCamera2Helper?.setPreviewFps(videoConfig.fps)
//        mCamera2Helper?.setImageAvailableListener(this)
        cameraXController.setUpCamera(context,mSurfaceProvider,videoConfig,this)
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
       cameraXController.unBindAll()
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
            val planes = image.planes
            val height = it.height
            val stride = planes[0].rowStride

//        LogUtils.i("myLog onImageAvailable: isLiving: $isLiving")


            if (nv21Rotated == null) {
                mNv12 = ByteArray(stride * height * 3 / 2)
                nv21Rotated = ByteArray(stride * height * 3 / 2)
//                LogUtils.i("myLog stride: $stride height: $height")
            }


            val nv21 = ImageUtils.yuv420ToNv21(image)

            ImageUtils.nv21_rotate_to_90(nv21, nv21Rotated, stride, image.height)
            SaveVideoByteFileUtils.writeNv21Bytes(mNv12)
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

    override fun analyze(image: ImageProxy) {

        val timestamp = System.currentTimeMillis()
//        val planes = image.planes
//        val height = image.height
//        val stride = planes[0].rowStride
//
////        LogUtils.i("myLog onImageAvailable: isLiving: $isLiving")
//
//
//        if (nv21Rotated == null) {
//            mNv12 = ByteArray(stride * height * 3 / 2)
//            nv21Rotated = ByteArray(stride * height * 3 / 2)
////                LogUtils.i("myLog stride: $stride height: $height")
//        }

//        val nv12Temp =   ConvertUtils.YUV_420_888toNV12(image,0)
//        SaveVideoByteFileUtils.writeNv21Bytes(nv12Temp)



//        val nv21 = ImageUtils.yuv420ToNv21(image)
//
//        ImageUtils.nv21_rotate_to_90(nv21, nv21Rotated, stride, image.height)
//        SaveVideoByteFileUtils.writeNv21Bytes(mNv12)
        if(isMediaCodec){
            val nv12Temp =   ConvertUtils.YUV_420_888toNV12(image,0)
//            SaveVideoByteFileUtils.writeNv21Bytes(nv12Temp)
            mVideoEncoder?.encode(nv12Temp,timestamp)
//            mNv12?.let { nv12 ->
//                //旋转后转nv12 目前硬编码接收的数据必须为NV12
////                val nv12Temp = ImageUtils.nv21toNV12(nv21Rotated, nv12)
//
//              val nv12Temp =   ConvertUtils.YUV_420_888toPortraitNV12(image,0)
//                mVideoEncoder?.encode(nv12Temp,timestamp)
////                    SaveVideoByteFileUtils.writeNv21Bytes(nv12Temp)
//            }
        }else{
//            val rtmpPackage = RTMPPackage()
//            rtmpPackage.isMediaCodec = false
//            rtmpPackage.buffer = nv21Rotated!!
//            rtmpPackage.type = RTMPPackage.RTMP_PACKET_TYPE_VIDEO
////                LogUtils.i("myLog --- rtmpPackage.buffer: ${rtmpPackage.buffer.size} ")
//            addPackage(rtmpPackage)
        }

        LogUtils.i("myLog analyze width: ${image.width} height: ${image.height} image.getImageInfo().getRotationDegrees(): ${image.getImageInfo().getRotationDegrees()}")
        image.close()
    }
}