package com.skyward.nativelivelib.utils;

import android.annotation.SuppressLint;
import android.media.Image;



import com.skyward.nativelivelib.YuvLib;
import com.skyward.nativelivelib.camera2.ICamera2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ConvertUtils {
    private static final String TAG = "ConvertUtils";
    private static byte[] mPortraitYUV;
    private static byte[] mLandscapeYUV;


    public static byte[] YUV_420_888toNV12(Image image, int rotation, ICamera2.CameraType cameraType) {
        if(cameraType == ICamera2.CameraType.BACK){
            if (90 == rotation) {
                return YUV_420_888toPortraitNV12(image, rotation);
            } else {
                return YUV_420_888toLandscapeNV12(image, rotation);
            }
        }else {
            if (270 == rotation) {
                return YUV_420_888toPortraitNV12(image, rotation);
            } else {
                return YUV_420_888toLandscapeNV12(image, rotation);
            }
        }
    }

    /**
     * YUV_420_888 to Landscape NV21
     *
     * @param image CameraX ImageProxy
     * @return nv12 byte array
     */
    @SuppressLint("UnsafeOptInUsageError")
    public static byte[] YUV_420_888toLandscapeNV12(Image image, int rotation) {
        byte [] bytes = YuvLib.convertToI420(image).asArray();
        if (null == mLandscapeYUV || mLandscapeYUV.length != bytes.length) {
            mLandscapeYUV = new byte[bytes.length];
        }
        YuvLib.I420ToNV12(bytes, image.getWidth(), image.getHeight(), mLandscapeYUV);
        return mLandscapeYUV;
    }

    /**
     * YUV_420_888 to Portrait NV12
     * @param image CameraX ImageProxy
     * @param rotation display rotation
     * @return nv12 byte array
     */
    @SuppressLint("UnsafeOptInUsageError")
    public static byte[] YUV_420_888toPortraitNV12(Image image, int rotation) {
        YuvFrame yuvFrame = YuvLib.convertToI420(image);

        //TODO optimization of vertical screen libyuv rotation
        byte[] bytes = YuvLib.rotate(yuvFrame, rotation).asArray();
//        SaveVideoByteFileUtils.writeNv21Bytes(bytes);
        if (null == mPortraitYUV || mPortraitYUV.length != bytes.length) {
            mPortraitYUV = new byte[bytes.length];
        }
        YuvLib.I420ToNV12(bytes, image.getWidth(), image.getHeight(), mPortraitYUV);
//        SaveVideoByteFileUtils.writeNv21Bytes(mPortraitYUV,"codec2");
        return mPortraitYUV;
    }





    @SuppressLint("UnsafeOptInUsageError")
    public static byte[] YUV_420_888toI420(Image image, int rotation) {
        YuvFrame yuvFrame = YuvLib.convertToI420(image);
        return YuvLib.rotate(yuvFrame, rotation).asArray();
    }


}
