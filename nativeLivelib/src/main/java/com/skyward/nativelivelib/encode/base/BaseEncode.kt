package com.skyward.nativelivelib.encode.base

/**
 * @author skyward
 * @date 2022/1/16 12:20
 * @desc
 *
 **/
interface BaseEncode {


    fun setPresentationTimeUs(presentationTimeUs:Long)

    fun setLiving(living:Boolean)

    fun encode(data:ByteArray,timestamp:Long)

    fun release()

    fun setEncodeResultListener(listener: RtmpPacketListener)
}