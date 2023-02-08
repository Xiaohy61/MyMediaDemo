package com.skyward.mymediademo

import android.media.AudioFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

import com.skyward.nativelivelib.camera2.AutoFitSurfaceView
import com.skyward.nativelivelib.camera2.Camera2Helper
import com.skyward.nativelivelib.config.AudioConfiguration
import com.skyward.nativelivelib.config.VideoConfiguration
import com.skyward.nativelivelib.push.PushManager

class MediaCodecPushActivity : AppCompatActivity() {
//    private val url = "rtmp://192.168.0.6:1935/rtmplive/skyward"

    private lateinit var mSurfaceView: AutoFitSurfaceView
    private lateinit var mPushManager: PushManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec_push)
        mSurfaceView = findViewById(R.id.surfaceView)


        val videoConfiguration = VideoConfiguration.Builder()
            .setSize(720, 1280)
            .setBps(400, 1300)
            .build()
        val audioConfiguration = AudioConfiguration.Builder()
            .build()
        mPushManager = PushManager(this)
        mPushManager.config(videoConfiguration,audioConfiguration,mSurfaceView,true)


        findViewById<Button>(R.id.btn_start_push).setOnClickListener {
            mPushManager.startLive(Config.url)
        }

        findViewById<Button>(R.id.btn_stop_push).setOnClickListener {
            mPushManager.stopLive()
        }
    }

    override fun onStop() {
        super.onStop()
       mPushManager.stopLive()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPushManager.release()

    }



}