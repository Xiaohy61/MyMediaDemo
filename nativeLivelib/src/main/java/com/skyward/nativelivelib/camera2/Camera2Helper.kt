package com.skyward.nativelivelib.camera2

import android.content.Context
import android.hardware.camera2.CaptureRequest
import com.blankj.utilcode.util.LogUtils
import com.skyward.nativelivelib.utils.ImageUtils

import java.util.concurrent.locks.ReentrantLock

/**
 * @author skyward
 * @date 2022/1/13 11:13
 * @desc
 *
 **/
class Camera2Helper(context: Context) : Camera2BaseHelper(context), ICamera2Helper {



    override fun setIso(iso: Int) {

        setCameraBuilderMode(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        setCameraBuilderMode(
            CaptureRequest.SENSOR_SENSITIVITY,
            iso
        )
        LogUtils.i("myLog iso: $iso")
        updatePreview()
    }

    override fun setShutter(shutter: Long) {
        setCameraBuilderMode(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        setCameraBuilderMode(
            CaptureRequest.SENSOR_EXPOSURE_TIME,
            shutter
        )
        LogUtils.i("myLog shutter: $shutter")
        updatePreview()
    }

    override fun setEv(ev: Int) {
        setCameraBuilderMode(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        setCameraBuilderMode(
            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
            ev
        )
        LogUtils.i("myLog ev: $ev")
        updatePreview()
    }

    override fun setWb(wb: Int) {

    }






}