package com.skyward.nativelivelib.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;



import com.blankj.utilcode.util.LogUtils;

import java.nio.ByteBuffer;

/**
 * @author skyward
 * @date 2022/1/16 17:39
 * @desc
 **/
public class ImageUtils {

    /**
     * YUV_420_888转NV21
     *
     * @param image CameraX ImageProxy
     * @return byte array
     */
    public static byte[] yuv420ToNv21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }
        else {
            for (; pos<ySize; pos+=width) {
                yBuffer.get(nv21, pos, width);
                yBuffer.position(yBuffer.position() + rowStride - width); // skip
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert(rowStride == image.getPlanes()[1].getRowStride());
        assert(pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            vBuffer.put(1, (byte)0);
            if (uBuffer.get(0) == 0) {
                vBuffer.put(1, (byte)255);
                if (uBuffer.get(0) == 255) {
                    vBuffer.put(1, savePixel);
                    vBuffer.get(nv21, ySize, uvSize);

                    return nv21; // shortcut
                }
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }


    public static byte[] nv21_rotate_to_90(byte[] nv21_data, byte[] nv21_rotated, int width, int height)
    {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;

        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1)*width;
        for (int x = 0; x < width; x++)
        {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }


    public  static byte[] nv21toNV12(byte[] nv21,byte[] nv12) {
        int size = nv21.length;
        nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }


    public static byte[] getDataFromImage(Image image) {
        int w = image.getWidth(), h = image.getHeight();
        int i420Size = w * h * 3 / 2;
        int picel1 = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        int picel2 = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888);

        Image.Plane[] planes = image.getPlanes();
        //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176
        int remaining0 = planes[0].getBuffer().remaining();
        int remaining1 = planes[1].getBuffer().remaining();
        //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1
        int remaining2 = planes[2].getBuffer().remaining();
        //获取pixelStride，可能跟width相等，可能不相等
        int pixelStride = planes[2].getPixelStride();
        int rowOffest = planes[2].getRowStride();
        byte[] nv21 = new byte[i420Size];
        byte[] yRawSrcBytes = new byte[remaining0];
        byte[] uRawSrcBytes = new byte[remaining1];
        byte[] vRawSrcBytes = new byte[remaining2];
        planes[0].getBuffer().get(yRawSrcBytes);
        planes[1].getBuffer().get(uRawSrcBytes);
        planes[2].getBuffer().get(vRawSrcBytes);
        if (pixelStride == w) {
            //两者相等，说明每个YUV块紧密相连，可以直接拷贝
            System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
            System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
        } else {
            byte[] ySrcBytes = new byte[w * h];
            byte[] uSrcBytes = new byte[w * h / 2 - 1];
            byte[] vSrcBytes = new byte[w * h / 2 - 1];
            for (int row = 0; row < h; row++) {
                //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);

                //y执行两次，uv执行一次
                if (row % 2 == 0) {
                    //最后一行需要减一
                    if (row == h - 2) {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                    } else {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                    }
                }
            }
            System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
            System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
        }
        return nv21;
    }


    //Planar格式（P）的处理
    private static ByteBuffer getuvBufferWithoutPaddingP(ByteBuffer uBuffer,ByteBuffer vBuffer, int width, int height, int rowStride, int pixelStride){
        int pos = 0;
        byte []byteArray = new byte[height*width/2];
        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                byteArray[pos++] = vBuffer.get(vuPos);
                byteArray[pos++] = uBuffer.get(vuPos);
            }
        }
        ByteBuffer bufferWithoutPaddings=ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray);
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip();
        return bufferWithoutPaddings;
    }
    //Semi-Planar格式（SP）的处理和y通道的数据
    private static ByteBuffer getBufferWithoutPadding(ByteBuffer buffer, int width, int rowStride, int times,boolean isVbuffer){
        if(width == rowStride) return buffer;  //没有buffer,不用处理。
        int bufferPos = buffer.position();
        int cap = buffer.capacity();
        byte []byteArray = new byte[times*width];
        int pos = 0;
        //对于y平面，要逐行赋值的次数就是height次。对于uv交替的平面，赋值的次数是height/2次
        for (int i=0;i<times;i++) {
            buffer.position(bufferPos);
            //part 1.1 对于u,v通道,会缺失最后一个像u值或者v值，因此需要特殊处理，否则会crash
            if(isVbuffer && i==times-1){
                width = width -1;
            }
            buffer.get(byteArray, pos, width);
            bufferPos+= rowStride;
            pos = pos+width;
        }

        //nv21数组转成buffer并返回
        ByteBuffer bufferWithoutPaddings=ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray);
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip();
        return bufferWithoutPaddings;
    }

    public static byte[] YUV_420_888toNV21(Image image) {
        int width =  image.getWidth();
        int height = image.getHeight();
        ByteBuffer yBuffer = getBufferWithoutPadding(image.getPlanes()[0].getBuffer(), image.getWidth(), image.getPlanes()[0].getRowStride(),image.getHeight(),false);
        ByteBuffer vBuffer;
        //part1 获得真正的消除padding的ybuffer和ubuffer。需要对P格式和SP格式做不同的处理。如果是P格式的话只能逐像素去做，性能会降低。
        if(image.getPlanes()[2].getPixelStride()==1){ //如果为true，说明是P格式。
            vBuffer = getuvBufferWithoutPaddingP(image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(),
                    width,height,image.getPlanes()[1].getRowStride(),image.getPlanes()[1].getPixelStride());
        }else{
            vBuffer = getBufferWithoutPadding(image.getPlanes()[2].getBuffer(), image.getWidth(), image.getPlanes()[2].getRowStride(),image.getHeight()/2,true);
        }

        //part2 将y数据和uv的交替数据（除去最后一个v值）赋值给nv21
        int ySize = yBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21;
        int byteSize = width*height*3/2;
        nv21 = new byte[byteSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);

        //part3 最后一个像素值的u值是缺失的，因此需要从u平面取一下。
        ByteBuffer uPlane = image.getPlanes()[1].getBuffer();
        byte lastValue = uPlane.get(uPlane.capacity() - 1);
        nv21[byteSize - 1] = lastValue;
        return nv21;
    }






}