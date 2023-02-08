package com.skyward.nativelivelib.encode.base

import com.skyward.nativelivelib.push.RTMPPackage

/**
 * @author skyward
 * @date 2022/1/16 12:20
 * @desc
 *
 **/
interface RtmpPacketListener {

    fun addPackage(rtmpPackage: RTMPPackage)
}