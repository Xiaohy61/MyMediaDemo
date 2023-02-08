package com.skyward.nativelivelib.config

import android.media.AudioFormat
import com.skyward.nativelivelib.config.AudioConfiguration
import android.media.MediaCodecInfo
import android.media.MediaFormat

/**
 * @Title: AudioConfiguration
 * @Package com.devyk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 */
class AudioConfiguration private constructor(builder: Builder) {
    val minBps: Int
    val maxBps: Int
    val sampleRate: Int
    val audioFormat: Int
    val channelCount: Int
    val adts: Int
    val aacProfile: Int
    val mime: String
    val aec: Boolean
    val mediaCodec: Boolean

    class Builder {
        var mediaCodec = DEFAULT_MEDIA_CODEC
        var minBps = DEFAULT_MIN_BPS
        var maxBps = DEFAULT_MAX_BPS
        var sampleRate = DEFAULT_FREQUENCY
        var audioFormat = DEFAULT_AUDIO_ENCODING
        var channelCount = DEFAULT_CHANNEL_COUNT
        var adts = DEFAULT_ADTS
        var mime = DEFAULT_MIME
        var aacProfile = DEFAULT_AAC_PROFILE
        var aec = DEFAULT_AEC
        fun setBps(minBps: Int, maxBps: Int): Builder {
            this.minBps = minBps
            this.maxBps = maxBps
            return this
        }

        fun setSampleRate(sampleRate: Int): Builder {
            this.sampleRate = sampleRate
            return this
        }

        fun setAudioFormat(audioFormat: Int): Builder {
            this.audioFormat = audioFormat
            return this
        }

        fun setChannelCount(channelCount: Int): Builder {
            this.channelCount = channelCount
            return this
        }

        fun setAdts(adts: Int): Builder {
            this.adts = adts
            return this
        }

        fun setAacProfile(aacProfile: Int): Builder {
            this.aacProfile = aacProfile
            return this
        }

        fun setMime(mime: String): Builder {
            this.mime = mime
            return this
        }

        fun setAec(aec: Boolean): Builder {
            this.aec = aec
            return this
        }

        fun setMediaCodec(mediaCodec: Boolean): Builder {
            this.mediaCodec = mediaCodec
            return this
        }

        fun build(): AudioConfiguration {
            return AudioConfiguration(this)
        }
    }

    companion object {
        const val DEFAULT_FREQUENCY = 44100
        const val DEFAULT_MAX_BPS = 64
        const val DEFAULT_MIN_BPS = 32
        const val DEFAULT_ADTS = 0
        const val DEFAULT_MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        const val DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val DEFAULT_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectERLC
        const val DEFAULT_CHANNEL_COUNT = 2
        const val DEFAULT_AEC = false
        const val DEFAULT_MEDIA_CODEC = true
        fun createDefault(): AudioConfiguration {
            return Builder().build()
        }
    }

    init {
        minBps = builder.minBps
        maxBps = builder.maxBps
        sampleRate = builder.sampleRate
        audioFormat = builder.audioFormat
        channelCount = builder.channelCount
        adts = builder.adts
        mime = builder.mime
        aacProfile = builder.aacProfile
        aec = builder.aec
        mediaCodec = builder.mediaCodec
    }
}