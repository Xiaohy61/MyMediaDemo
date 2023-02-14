package com.skyward.mymediademo

import com.skyward.nativelivelib.camera2.ICamera2

/**
 * @author skyward
 * @date 2023/2/7 23:24
 * @desc
 *
 **/
object Config {
//    val url = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_514567206_71926781&key=23aad5b8b2ae91730ad4ca27f026257f&schedule=rtmp&pflag=1"
    val url = "rtmp://192.168.16.65/rtmplive/skyward"

    var cameraType = ICamera2.CameraType.BACK

    var width = 720
    var height =1280
}