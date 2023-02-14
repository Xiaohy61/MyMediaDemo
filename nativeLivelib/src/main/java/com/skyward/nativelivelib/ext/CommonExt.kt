package com.skyward.nativelivelib.ext

import android.content.Context
import android.view.Surface
import android.view.WindowManager
import com.skyward.nativelivelib.camera2.ICamera2

/**
 * @author skyward
 * @date 2023/2/13 17:39
 * @desc
 *
 **/

fun Context.getCameraRotation(cameraType: ICamera2.CameraType):Int{
    val rotation = when ((getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
       //竖屏
        Surface.ROTATION_0 ,Surface.ROTATION_180-> {
            if(cameraType == ICamera2.CameraType.BACK){
                90
            }else{
                //前摄
                270
            }
        }
        //横屏
        Surface.ROTATION_90 ,Surface.ROTATION_270 ->{
//            if(cameraType == ICamera2.CameraType.BACK){
//                0
//            }else{
//                0
//            }
            0
        }
        else -> {
            90
        }
    }
    return rotation
}