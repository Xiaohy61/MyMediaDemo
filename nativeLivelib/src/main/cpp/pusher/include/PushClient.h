//
// Created by skyward on 2022/1/9.
//

#ifndef MYLIVE_PUSHCLIENT_H
#define MYLIVE_PUSHCLIENT_H
#include <jni.h>
#include "RtmpManager.h"
#include "VideoChannel.h"
#include "AudioChannel.h"
#include "PushCallback.h"

class PushClient {

public:
    void init(JavaVM *javaVm,JNIEnv *jniEnv,bool  isMediaCodec,jobject obj);
    int isStart();
    int isReadyPushing();
    void release();
    void stop();
    void start(const char *path);
    void restartPush();
    void setMediaCodec(int mediacodec);

    VideoChannel *videoChannel = nullptr;
    AudioChannel *audioChannel = nullptr;
    RtmpManager *rtmpManager = nullptr;


};


#endif //MYLIVE_PUSHCLIENT_H
