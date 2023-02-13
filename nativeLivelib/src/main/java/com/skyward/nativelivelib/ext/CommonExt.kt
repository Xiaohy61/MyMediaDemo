package com.skyward.nativelivelib.ext

import android.content.Context
import android.view.Surface
import android.view.WindowManager

/**
 * @author skyward
 * @date 2023/2/13 17:39
 * @desc
 *
 **/

fun Context.getCameraRotation():Int{
    val rotation = when ((getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
        Surface.ROTATION_0 ,Surface.ROTATION_180-> 90
        Surface.ROTATION_90 ,Surface.ROTATION_270 -> 0
        else -> {
            90
        }
    }
    return rotation
}