package com.skyward.nativelivelib.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.AttributeSet
import android.view.View.MeasureSpec
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils


/**
 * @author skyward
 * @date 2022/1/27 15:34
 * @desc
 *
 **/
class PlayGLSurfaceView : GLSurfaceView {

    private lateinit var mPlayRender: PlayRender
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var isNeedRequestLayout = true
    private var canRender = false
    private var mHandler = Handler(Looper.getMainLooper())

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        setEGLContextClientVersion(2)
        mPlayRender = PlayRender(context)
        setRenderer(mPlayRender)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

//        if(mVideoWidth == 0 && mVideoHeight == 0){
//            isNeedRequestLayout = true
//        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            if (width < height * mVideoWidth / mVideoHeight) {
                setMeasuredDimension(width, width * mVideoHeight / mVideoWidth)
            } else {
                setMeasuredDimension(height * mVideoWidth / mVideoHeight, height)
            }
//            setMeasuredDimension(mVideoWidth, mVideoHeight)
            LogUtils.i("myLog mVideoWidth: $mVideoWidth mVideoHeight: $mVideoHeight")
        } else {
            setMeasuredDimension(width, height)
        }

//        setMeasuredDimension(ConvertUtils.dp2px(368f),ConvertUtils.dp2px(384f))
    }


    fun setAspectRatio(width: Int, height: Int) {
        this.mVideoWidth = width
        this.mVideoHeight = height
        LogUtils.i("myLog setVideoWidthAndHeight width: $width  height: $height")
        mHandler.post {
            requestLayout()
        }
    }

    fun setYuvData(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray) {


//        LogUtils.i("myLog setYuvData -- width: $width height: $height y: ${y.size} u:${u.size} v:${v.size}")

        requestRender()
        mPlayRender.setYUVRenderData(width, height, y, u, v)
//        if(canRender){
//            LogUtils.i("myLog ----canRender---")
//
//        }
    }
}