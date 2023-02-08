package com.skyward.nativelivelib.camera2

import android.media.ImageReader
import android.view.TextureView

/**
 * @author skyward
 * @date 2022/1/12 14:53
 * @desc
 *
 **/
interface ICamera2 {

    /**
     * 打开摄像头
     */
    fun openCamera(cameraType: CameraType)

    /**
     * 切换摄像头
     */
    fun switchCamera(cameraType: CameraType)

    /**
     * 设置surfaceView 预览的view
     */
    fun setSurfaceView(surfaceView: AutoFitSurfaceView)

    /**
     * 设置surfaceView 预览的view
     */
    fun setSurfaceView(surfaceView: AutoFitSurfaceView,width:Int,height:Int)


    /**
     * 开启预览
     */
    fun startPreview()

    /**
     * 更新预览界面
     */
    fun updatePreview()

    /**
     * 关闭摄像头
     */
    fun closeCamera()

    fun setImageAvailableListener(imageAvailableListener: ImageReader.OnImageAvailableListener)

    /**
     * 摄像头类型
     */
    enum class CameraType{
        FRONT,
        BACK
    }

}