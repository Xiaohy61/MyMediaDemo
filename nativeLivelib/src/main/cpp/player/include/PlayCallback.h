//
// Created by skyward on 2022/1/24.
//

#ifndef MYMEDIADEMO_PLAYCALLBACK_H
#define MYMEDIADEMO_PLAYCALLBACK_H
#include "jni.h"
#include <linux/stddef.h>
#include "MyLog.hpp"
#include "Constants.h"

class PlayCallback {
public:

    PlayCallback(JavaVM *javaVm,JNIEnv *env,jobject obj);
    void onCallPrepared(int type);
    void onCallVideoInfo(int type,int width,int height);
    void onCallTimeInfo(int type,int curr,int total);
    void onCallLoad(int type,bool load);
    void onCallRenderYUV(int width,int height,uint8_t *fy,uint8_t *fu,uint8_t *fv);

    ~PlayCallback();


private:
    JavaVM *javaVm = nullptr;
    JNIEnv *jniEnv = nullptr;
    jobject obj;

    jmethodID jMid_prepared;
    jmethodID jMid_videoInfo;
    jmethodID jMid_timeInfo;
    jmethodID jMid_load;
    jmethodID jMid_renderYuv;
};


#endif //MYMEDIADEMO_PLAYCALLBACK_H
