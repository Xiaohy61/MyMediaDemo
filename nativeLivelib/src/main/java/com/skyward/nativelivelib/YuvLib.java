package com.skyward.nativelivelib;

import android.media.Image;

import com.blankj.utilcode.util.LogUtils;
import com.skyward.nativelivelib.utils.YuvFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class YuvLib {

    private static final ArrayList<YuvFrame> mYuvFrameList = new ArrayList<>();
    private static final ArrayList<Integer> mWidthList = new ArrayList<>();

    static {
        System.loadLibrary("pushlivelib");
    }


    public static YuvFrame createYuvFrame(int width, int height) {

        if (!mWidthList.contains(width)) {
            int ySize = width * height;
            int uvSize = width * height / 4;
            int extra = (width % 2 == 0) ? 0 : 1;
            mWidthList.add(width);
            mYuvFrameList.add(new YuvFrame(
                    ByteBuffer.allocateDirect(ySize),
                    ByteBuffer.allocateDirect(uvSize),
                    ByteBuffer.allocateDirect(uvSize),
                    width, width / 2 + extra,
                    width / 2 + extra, width, height));
            LogUtils.i("myLog mYuvFrameList.width: "+width+" height: "+height);
        }
//
        int index = 0;
        for (int i = 0; i < mYuvFrameList.size(); i++) {
            if(mYuvFrameList.get(i).getWidth() == width){
                index = i;
            }
        }
        return mYuvFrameList.get(index);
    }

    public static YuvFrame createRotateYuvFrame(int width, int height, int rotationMode) {
        int outWidth = (rotationMode == 90 || rotationMode == 270) ? height : width;
        int outHeight = (rotationMode == 90 || rotationMode == 270) ? width : height;
        return createYuvFrame(outWidth, outHeight);
    }

    public static YuvFrame rotate(Image image, int rotationMode) {
        assert (rotationMode == 0 || rotationMode == 90 || rotationMode == 180 || rotationMode == 270);
        YuvFrame outFrame = createRotateYuvFrame(image.getWidth(), image.getHeight(), rotationMode);
        rotate(image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), image.getWidth(), image.getHeight(), rotationMode);
        return outFrame;
    }

    public static YuvFrame rotate(YuvFrame yuvFrame, int rotationMode) {
        assert (rotationMode == 0 || rotationMode == 90 || rotationMode == 180 || rotationMode == 270);
        YuvFrame outFrame = createRotateYuvFrame(yuvFrame.getWidth(), yuvFrame.getHeight(), rotationMode);

        rotate(yuvFrame.getY(), yuvFrame.getU(), yuvFrame.getV(), yuvFrame.getyStride(),
                yuvFrame.getuStride(), yuvFrame.getvStride(), outFrame.getY(), outFrame.getU(),
                outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(),
                yuvFrame.getWidth(), yuvFrame.getHeight(), rotationMode);
//
        return outFrame;
    }

    public static YuvFrame convertToI420(Image image) {
        YuvFrame outFrame = createYuvFrame(image.getWidth(), image.getHeight());
//        LogUtils.i("myLog pre convertToI420 outFrame y size: " + outFrame.getWidth());
        convertToI420(
                image.getPlanes()[0].getBuffer(),
                image.getPlanes()[1].getBuffer(),
                image.getPlanes()[2].getBuffer(),
                image.getPlanes()[0].getRowStride(),
                image.getPlanes()[1].getRowStride(),
                image.getPlanes()[2].getRowStride(),
                image.getPlanes()[2].getPixelStride(),
                outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(),
                outFrame.getuStride(), outFrame.getvStride(),
                image.getWidth(), image.getHeight());
//        LogUtils.i("myLog after convertToI420 outFrame y size: " + outFrame.getWidth());
        return outFrame;
    }

    public static YuvFrame convertToI420(YuvFrame yuvFrame, int uvPixelStride) {
        YuvFrame outFrame = createYuvFrame(yuvFrame.getWidth(), yuvFrame.getHeight());
        convertToI420(yuvFrame.getY(), yuvFrame.getU(), yuvFrame.getV(), yuvFrame.getyStride(), yuvFrame.getuStride(), yuvFrame.getvStride(), uvPixelStride, outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), yuvFrame.getWidth(), yuvFrame.getHeight());
        return outFrame;
    }

    public static native void rotate(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride, int vStride, ByteBuffer yOut, ByteBuffer uOut, ByteBuffer vOut, int yOutStride, int uOutStride, int vOutStride, int width, int height, int rotationMode);

    public static native void convertToI420(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride,
                                            int vStride, int srcPixelStrideUv, ByteBuffer yOut, ByteBuffer uOut,
                                            ByteBuffer vOut, int yOutStride, int uOutStride, int vOutStride,
                                            int width, int height);


    public static native void I420ToNV12(byte[] i420Src, int width, int height, byte[] nv12Dst);

    public static native void NV21ToI420(byte[] nv21, int width, int height, byte[] i420);
}
