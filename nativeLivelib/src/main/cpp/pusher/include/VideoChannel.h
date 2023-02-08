//
// Created by skyward on 2022/1/9.
//

#ifndef MYLIVE_VIDEOCHANNEL_H
#define MYLIVE_VIDEOCHANNEL_H


#include <pthread.h>
#include <x264.h>
#include "PushCallback.h"
#include "safe_queue.h"



extern "C"{
#include <rtmp.h>
}

class VideoChannel {
  typedef void (*VideoEncoderCallback)(RTMPPacket *packet);

public:
    VideoChannel();
    void setVideoCallback(VideoEncoderCallback videoCallback);

    void openVideoCodec(int width,int height,int fps,int bit);
    void encodeData(int8_t *data);
    VideoChannel(PushCallback *pushCallback,int isMediaCodec);
    void startEncoder();
    void onEncoder();
    int isStart = 0;
    SafeQueue<int8_t*> mVideoPackets;
    void restart();
    void stop();
    void sendSpsPps(uint8_t *sps,uint8_t *pps,int sps_len,int pps_len);
    void sendX264Frame(int type,uint8_t *payload,int i_playload,long i);
    void sendMediaCodecEncodeFrame(uint8_t *data,int len,long  tms);
    void sendH264(int type,uint8_t *data,int len,long tms);
    void getH264SpsPpsInfo(uint8_t *data,int len);
    void setMediaCodec(int mediacodec);
    void release();
    ~VideoChannel();

private:
    int mWidth,mHeight,mFps,mBit,mY_Size,mUV_Size;
    x264_t *mVideoCodec = nullptr;
    x264_picture_t *pic_in = nullptr;
    VideoEncoderCallback mVideoCallback;
    PushCallback *mPushCallback;
    pthread_t mPid;
    int isMediaCodec = 0;

    //硬编后获取的sps 和pps 信息
    int16_t sps_len;
    uint8_t *sps;
    int16_t pps_len;
    uint8_t *pps;
    //---------------------

};


#endif //MYLIVE_VIDEOCHANNEL_H
