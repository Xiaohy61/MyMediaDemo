//
// Created by skyward on 2022/1/9.
//

#include "include/PushCallback.h"

PushCallback::PushCallback(JavaVM *javaVm, JNIEnv *env, jobject obj) {
    this->javaVm = javaVm;
    this->jniEnv = env;
    //全局引用
    this->obj = env->NewGlobalRef(obj);

    //拿到java层的methodId
    jclass javaClass = env->GetObjectClass(obj);
    this->mConnectId = env->GetMethodID(javaClass,"onRtmpConnect","()V");
    this->mSucceedId =env->GetMethodID(javaClass,"onRtmpConnectSuccess","()V");
    this->mErrorId = env->GetMethodID(javaClass,"onError","(I)V");
}

void PushCallback::onRtmpConnect(int thread_mode) {
    if(thread_mode == THREAD_MAIN){
        //主线程直接调用java方法
        this->jniEnv->CallVoidMethod(this->obj,this->mConnectId);
    } else{
        //native的子线程，需要把该线程 Attach到JavaVM上去才可以获取JNIEnv，然后去调用java方法
        JNIEnv *jniEnv1 = nullptr;
        jint ret = javaVm->AttachCurrentThread(&jniEnv1,0);
        if(ret != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(this->obj,mConnectId);
        javaVm->DetachCurrentThread();
    }
}

void PushCallback::onRtmpSucceed(int thread_mode) {
    if(thread_mode == THREAD_MAIN){
        this->jniEnv->CallVoidMethod(this->obj,mSucceedId);
    } else{
        JNIEnv *jniEnv1 = nullptr;
        jint  ret = javaVm->AttachCurrentThread(&jniEnv1,0);
        if(ret != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(this->obj,mSucceedId);
        javaVm->DetachCurrentThread();
    }
}

void PushCallback::OnError(int thread_mode, int errCode) {
    if(thread_mode == THREAD_MAIN){
        this->jniEnv->CallVoidMethod(this->obj,this->mErrorId);
    } else{
        JNIEnv *jniEnv1 = nullptr;
        jint  ret = javaVm->AttachCurrentThread(&jniEnv1,0);
        if(ret != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(this->obj,mErrorId,errCode);
        javaVm->DetachCurrentThread();
    }
}

PushCallback::~PushCallback() {
    this->javaVm = nullptr;
    //释放全局引用
    jniEnv->DeleteGlobalRef(this->obj);
    this->obj = nullptr;
    jniEnv = nullptr;
}
