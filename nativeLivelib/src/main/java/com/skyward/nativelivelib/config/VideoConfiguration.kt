package com.skyward.nativelivelib.config

import com.skyward.nativelivelib.config.VideoConfiguration

/**
 * @Title: VideoConfiguration
 * @Package com.devyk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 */
class VideoConfiguration private constructor(builder: Builder) {
    val height: Int
    val width: Int
    val minBps: Int
    val maxBps: Int
    val fps: Int
    val mediaCodec: Boolean
    val ifi: Int
    val mime: String

    class Builder {
         var mediaCodec = DEFAULT_MEDIA_CODEC
         var height = DEFAULT_HEIGHT
         var width = DEFAULT_WIDTH
         var minBps = DEFAULT_MIN_BPS
         var maxBps = DEFAULT_MAX_BPS
         var fps = DEFAULT_FPS
         var ifi = DEFAULT_IFI
         var mime = DEFAULT_MIME
        fun setSize(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return this
        }

        fun setBps(minBps: Int, maxBps: Int): Builder {
            this.minBps = minBps
            this.maxBps = maxBps
            return this
        }

        fun setFps(fps: Int): Builder {
            this.fps = fps
            return this
        }

        fun setIfi(ifi: Int): Builder {
            this.ifi = ifi
            return this
        }

        fun setMime(mime: String): Builder {
            this.mime = mime
            return this
        }

        fun setMediaCodec(mediaCodec: Boolean): Builder {
            this.mediaCodec = mediaCodec
            return this
        }

        fun build(): VideoConfiguration {
            return VideoConfiguration(this)
        }
    }

    companion object {
        const val DEFAULT_HEIGHT = 1920
        const val DEFAULT_WIDTH = 1080
        const val DEFAULT_FPS = 15
        const val DEFAULT_MAX_BPS = 1300
        const val DEFAULT_MIN_BPS = 400
        const val DEFAULT_IFI = 2
        const val DEFAULT_MIME = "video/avc"
        const val DEFAULT_MEDIA_CODEC = true
        fun createDefault(): VideoConfiguration {
            return Builder().build()
        }
    }

    init {
        height = builder.height
        width = builder.width
        minBps = builder.minBps
        maxBps = builder.maxBps
        fps = builder.fps
        ifi = builder.ifi
        mime = builder.mime
        mediaCodec = builder.mediaCodec
    }
}