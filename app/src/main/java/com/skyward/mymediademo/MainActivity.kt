package com.skyward.mymediademo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.blankj.utilcode.util.LogUtils

class MainActivity : AppCompatActivity() {
    private val url = "rtmp://192.168.0.6:1935/rtmplive/skyward"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




//        val nativeLib = PushLib()
//        findViewById<TextView>(R.id.tv_name).text = nativeLib.stringFromJNI()

        checkPermission()
        findViewById<Button>(R.id.btnMediaCodec).setOnClickListener {
            val intent = Intent(applicationContext,MediaCodecPushActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSoft).setOnClickListener {
            val intent = Intent(applicationContext,SoftPushActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            val intent = Intent(applicationContext,PlayVideoActivity::class.java)
            startActivity(intent)
        }


    }

   private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ), 1
            )
        }
        return false
    }

    override fun onResume() {
        super.onResume()
    }
}