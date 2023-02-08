package com.skyward.nativelivelib.camera2

import android.media.Image
import android.util.Size
import java.nio.ByteBuffer

/**
 * @author skyward
 * @date 2022/1/13 15:26
 * @desc
 *
 **/
interface ICamera2Helper {


    /**
     * iso
     */
    fun setIso(iso:Int)

    /**
     * ae 曝光时间，快门时间
     */
    fun setShutter(shutter:Long)

    fun setEv(ev:Int)

    fun setWb(wb:Int)


    fun setPreviewFps(fps:Int)



}