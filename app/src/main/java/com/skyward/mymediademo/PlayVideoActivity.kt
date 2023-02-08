package com.skyward.mymediademo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.skyward.nativelivelib.opengl.PlayGLSurfaceView
import com.skyward.nativelivelib.player.PlayManager
import com.skyward.nativelivelib.player.PlayVideoListener

class PlayVideoActivity : AppCompatActivity() {

    val playManager = PlayManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_video)

        val glPlayer = findViewById<PlayGLSurfaceView>(R.id.glPlayer)

        playManager.setGLSurfaceView(glPlayer)
        playManager.setUrl("http://mn.maliuedu.com/music/input.mp4")

        findViewById<Button>(R.id.btn_play).setOnClickListener {
            playManager.startPlay()
        }


        playManager.setPlayListener(object :PlayVideoListener{
            override fun onPrepare() {
                ToastUtils.showShort("准备好了")
            }
        })
    }
}