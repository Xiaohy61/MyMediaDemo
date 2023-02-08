#include <__bit_reference>
//
// Created by skyward on 2022/1/9.
//

#ifndef MYLIVE_RTMPMANAGER_H
#define MYLIVE_RTMPMANAGER_H


#include "safe_queue.h"
#include "PushCallback.h"
#include "AudioChannel.h"
#include "VideoChannel.h"
#include "MyLog.hpp"
extern "C"{
#include <rtmp.h>
}

class RtmpManager {

public:
    //rtmp packet 队列
    SafeQueue<RTMPPacket *> mPackets;
    //是否开始推流
    int isStart = false;

    RtmpManager(PushCallback *pushCallback,VideoChannel *videoChannel,AudioChannel *audioChannel,int mediaCodec);
    void setPushUrl(const char *url);
    void onConnect();
    void onPush();
    void release();
    void restart();
    void stop();
    void setMediaCodec(int mediaCodec);

    uint32_t mStartTime = 0;
    int readyPushing = false;
    RTMP *rtmp  = nullptr;




    ~RtmpManager();

private:
    char *url = nullptr;
    PushCallback *pushCallback;
    pthread_t mPid = 0;

    VideoChannel *videoChannel;
    AudioChannel *audioChannel;

     void setPacketReleaseCallback();

    int isMediaCodec = false;


};


#endif //MYLIVE_RTMPMANAGER_H
