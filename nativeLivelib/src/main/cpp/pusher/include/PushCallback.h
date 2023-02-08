//
// Created by skyward on 2022/1/9.
//

#ifndef MYLIVE_PUSHCALLBACK_H
#define MYLIVE_PUSHCALLBACK_H
#include <jni.h>
#include "Constants.h"

class PushCallback {

public:
    PushCallback(JavaVM *javaVm,JNIEnv *env,jobject obj);
    void onRtmpConnect(int thread_mode);
    void onRtmpSucceed(int thread_mode);
    void OnError(int thread_mode,int errCode);
    ~PushCallback();

private:
    JavaVM *javaVm = nullptr;
    JNIEnv *jniEnv = nullptr;
    jobject  obj;

    //回调
    jmethodID  mConnectId;
    jmethodID mSucceedId;
    jmethodID mErrorId;

};


#endif //MYLIVE_PUSHCALLBACK_H
