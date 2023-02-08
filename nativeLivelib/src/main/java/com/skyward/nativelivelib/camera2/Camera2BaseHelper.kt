package com.skyward.nativelivelib.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.skyward.nativelivelib.config.VideoConfiguration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author skyward
 * @date 2022/1/12 14:53
 * @desc
 *
 **/
abstract class Camera2BaseHelper(val context: Context) : ICamera2 {

    private val TAG = "myLog"
    private val cameraThread = HandlerThread("Camera2Thread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val imageReaderThread = HandlerThread("ImageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    private val mMainHandler = Handler(Looper.getMainLooper())
    private val cameraManager: CameraManager
    private val mCameraIds: Array<String>
    private val mIsCameraOpen: AtomicBoolean
    private lateinit var mCharacteristics: CameraCharacteristics
    private var mSensorOrientation = 0
    private var mCameraId = ""
    private var mCameraDevice: CameraDevice? = null
    private var mImageReader: ImageReader? = null
    private var mPreviewSession: CameraCaptureSession? = null
    private var mSurfaceView: AutoFitSurfaceView? = null
    private var width = VideoConfiguration.DEFAULT_WIDTH
    private var height = VideoConfiguration.DEFAULT_HEIGHT
    private val maxImages = 3
    private var mMatchSize = Size(height, width)
    private lateinit var mPreViewSurface: Surface
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mOnImageAvailableListener: ImageReader.OnImageAvailableListener? = null


    init {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraIds = cameraManager.cameraIdList
        mIsCameraOpen = AtomicBoolean(false)
    }

    override fun openCamera(cameraType: ICamera2.CameraType) {
        if (mIsCameraOpen.get()) {
            return
        }
        mIsCameraOpen.set(true)

        if (mSurfaceView == null) {
            requireNotNull(mSurfaceView) {
                "preview is null set"
            }
            return
        }
        mSurfaceView!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

                var cameraTypeId = CameraCharacteristics.LENS_FACING_BACK
                cameraTypeId = when (cameraType) {
                    ICamera2.CameraType.FRONT -> {
                        CameraCharacteristics.LENS_FACING_FRONT
                    }
                    ICamera2.CameraType.BACK -> {
                        CameraCharacteristics.LENS_FACING_BACK
                    }
                }
                for (cameraId in mCameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    //获取摄像头id
                    val lensFacing = characteristics[CameraCharacteristics.LENS_FACING]
//                    LogUtils.i(TAG, "lensFacing  ${lensFacing}")
                    //获取到的摄像头不是选择的摄像头就跳过
                    if (lensFacing != null && lensFacing != cameraTypeId) {
                        continue
                    }
                    mCharacteristics = characteristics
                    //初始化预览尺寸
                    initSize()
                    //初始化ImageReader
                    initImageReader()
                    //获取设头像角度
                    mSensorOrientation =
                        mCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION] as Int
                    this@Camera2BaseHelper.mCameraId = cameraId
                    //找到就退出，避免获取到的后摄不是主的后摄
                    break
                }
                //打开设头像
                mMainHandler.post {
                    openCamera(mCameraId)
                }

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

        })
    }


    private fun initSize() {
        val previewSize = getPreviewOutputSize(
            mSurfaceView!!.display,
            mCharacteristics,
            SurfaceHolder::class.java,
            SmartSize(height, width)
        )
        mMatchSize = previewSize
//        LogUtils.i(TAG,"previewSize: width: ${previewSize.width} height: ${previewSize.height}")
        mSurfaceView!!.setAspectRatio(previewSize.width, previewSize.height)
//        LogUtils.i(TAG,"mSurfaceView width:  ${mSurfaceView!!.width} height: ${mSurfaceView!!.height}")
    }

    private fun initImageReader() {
        mImageReader?.apply {
            close()
        }
        mImageReader = ImageReader.newInstance(
            mMatchSize.width,
            mMatchSize.height,
            ImageFormat.YUV_420_888,
            maxImages
        )



        mOnImageAvailableListener?.let {
            mImageReader?.setOnImageAvailableListener(it, imageReaderHandler)
        }
    }


    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String): Boolean {
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    startPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    mCameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    mCameraDevice = null
                }
            }, cameraHandler)

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }


    override fun switchCamera(cameraType: ICamera2.CameraType) {
        closeCamera()
        openCamera(cameraType)
    }

    override fun setSurfaceView(surfaceView: AutoFitSurfaceView) {
        this.mSurfaceView = surfaceView
    }

    override fun setSurfaceView(surfaceView: AutoFitSurfaceView, width: Int, height: Int) {
        this.mSurfaceView = surfaceView
        this.width = width
        this.height = height
    }

    /**
     * 预览数据回调
     */
    override fun setImageAvailableListener(imageAvailableListener: ImageReader.OnImageAvailableListener) {
        this.mOnImageAvailableListener = imageAvailableListener
    }

    override fun startPreview() {
        if (mSurfaceView == null && mCameraDevice == null) {
            return
        }
        mPreViewSurface = mSurfaceView!!.holder.surface
        val targets = listOf(mPreViewSurface, mImageReader!!.surface)
        mPreviewBuilder =
            mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                //预览输出目标
                addTarget(mPreViewSurface)
            }
        mPreviewBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        //输出到到文件，回调到setOnImageAvailableListener
        mPreviewBuilder.addTarget(mImageReader!!.surface)

        //创建camera session
        createCaptureSession(mCameraDevice!!, targets, cameraHandler)
    }

    private fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ) {
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                mPreviewSession = cameraCaptureSession
                updatePreview()
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            }

        }, handler)
    }

    override fun updatePreview() {
        try {
            mPreviewSession?.setRepeatingRequest(mPreviewBuilder.build(), null, cameraHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置 mPreviewBuilder 种类
     */
    open fun <T> setCameraBuilderMode(key: CaptureRequest.Key<T>, value: T) {
        mPreviewBuilder.set(key, value)

    }


    override fun closeCamera() {
        mIsCameraOpen.set(false)
        closePreviewSession()
        mCameraDevice?.apply {
            close()
            mCameraDevice = null
        }
        mImageReader?.apply {
            close()
            mImageReader = null
        }
        cameraHandler.removeCallbacksAndMessages(null)
        imageReaderHandler.removeCallbacksAndMessages(null)
        mMainHandler.removeCallbacksAndMessages(null)
        cameraThread.quit()
        imageReaderThread.quit()
    }

    private fun closePreviewSession() {
        mPreviewSession?.apply {
            close()
            mPreviewSession = null
        }
    }

}