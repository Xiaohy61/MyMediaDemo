package com.skyward.nativelivelib.push

class RTMPPackage {
    lateinit var buffer: ByteArray
    var tms: Long = 0
    var isMediaCodec = true

    //    视频包 音频包
    var type = 0

    constructor(buffer: ByteArray, tms: Long,isMediaCodec:Boolean) {
        this.buffer = buffer
        this.tms = tms
        this.isMediaCodec = isMediaCodec
    }



    constructor() {}

    companion object {
        const val RTMP_PACKET_TYPE_AUDIO_DATA = 2
        const val RTMP_PACKET_TYPE_AUDIO_HEAD = 1
        const val RTMP_PACKET_TYPE_VIDEO = 0
    }
}