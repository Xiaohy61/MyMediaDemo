package me.xcyoung.opengl.camera.widget.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.skyward.nativelivelib.config.VideoConfiguration
import java.util.concurrent.Executors

/**
 * @author ChorYeung
 * @since 2021/11/24
 */
class CameraXController {

    private var mCameraProvider:ProcessCameraProvider?= null

    fun setUpCamera(context: Context,
                    surfaceProvider: Preview.SurfaceProvider,
                    videoConfiguration: VideoConfiguration,
                    previewCallback:ImageAnalysis.Analyzer) {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            mCameraProvider = cameraProviderFuture.get()


            val preview: Preview = Preview.Builder()
                .setTargetResolution(Size(videoConfiguration.width,videoConfiguration.height))
                .build()




            val builder = ImageAnalysis.Builder()

            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(videoConfiguration.fps, videoConfiguration.fps)
            )
            builder.setTargetResolution(Size(videoConfiguration.width,videoConfiguration.height))

            val imageAnalysis = builder.build()
            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),previewCallback)

            preview.setSurfaceProvider(surfaceProvider)



            mCameraProvider?.unbindAll()
            val camera =
                mCameraProvider?.bindToLifecycle(
                    context as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
//            val cameraInfo = camera.cameraInfo
//            val cameraControl = camera.cameraControl
        }, ContextCompat.getMainExecutor(context))
    }


    fun unBindAll(){

    }


}