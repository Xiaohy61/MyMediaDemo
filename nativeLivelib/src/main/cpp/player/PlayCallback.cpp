//
// Created by skyward on 2022/1/24.
//

#include "include/PlayCallback.h"

PlayCallback::PlayCallback(JavaVM *javaVm, JNIEnv *env, jobject obj) {
    this->javaVm = javaVm;
    this->jniEnv = env;
    this->obj = env->NewGlobalRef(obj);


    jclass javaClass = jniEnv->GetObjectClass(obj);
    if(!javaClass){
        LOGI("myLog --- PlayCallback get class error");
        return;
    }

    jMid_videoInfo = env->GetMethodID(javaClass,"onCallVideoWidthAndHeight","(II)V");
    jMid_prepared = env->GetMethodID(javaClass,"onCallPrepared","()V");
    jMid_timeInfo = env->GetMethodID(javaClass,"onCallTimeInfo","(II)V");
    jMid_load = env->GetMethodID(javaClass,"onCallLoad","(Z)V");
    jMid_renderYuv = env->GetMethodID(javaClass,"onCallRenderYUV","(II[B[B[B)V");
}



void PlayCallback::onCallPrepared(int type) {
    if(type == THREAD_MAIN){
        jniEnv->CallVoidMethod(obj,jMid_prepared);
    } else{
        JNIEnv *jniEnv1;
        if(javaVm->AttachCurrentThread(&jniEnv1,0) != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(obj,jMid_prepared);
        javaVm->DetachCurrentThread();
    }
}

void PlayCallback::onCallVideoInfo(int type,int width, int height) {
    if(type == THREAD_MAIN){
        jniEnv->CallVoidMethod(obj,jMid_videoInfo,width,height);
    } else{
        JNIEnv *jniEnv1;
        if(javaVm->AttachCurrentThread(&jniEnv1,0) != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(obj,jMid_videoInfo,width,height);
        javaVm->DetachCurrentThread();
    }
}

void PlayCallback::onCallTimeInfo(int type, int curr, int total) {
    if(type == THREAD_MAIN){
        jniEnv->CallVoidMethod(obj,jMid_timeInfo,curr,total);
    } else{
        JNIEnv *jniEnv1;
        if(javaVm->AttachCurrentThread(&jniEnv1,0) != JNI_OK){
            return;
        }
        jniEnv1->CallVoidMethod(obj,jMid_timeInfo,curr,total);
        javaVm->DetachCurrentThread();
    }
}

void PlayCallback::onCallLoad(int type, bool load) {
  if(type == THREAD_MAIN){
      jniEnv->CallVoidMethod(obj,jMid_load,load);
  } else{
      JNIEnv *jniEnv1;
      if(javaVm->AttachCurrentThread(&jniEnv1,0) != JNI_OK){
          return;
      }
      jniEnv1->CallVoidMethod(obj,jMid_load,load);
      javaVm->DetachCurrentThread();
  }
}

void PlayCallback::onCallRenderYUV(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv) {
    JNIEnv *jniEnv1;
    if(javaVm->AttachCurrentThread(&jniEnv1,0) != JNI_OK){
        return;
    }
    jbyteArray y = jniEnv1->NewByteArray(width*height);
    jniEnv1->SetByteArrayRegion(y, 0,width*height, reinterpret_cast<const jbyte *>(fy));

    jbyteArray u = jniEnv1->NewByteArray(width*height/4);
    jniEnv1->SetByteArrayRegion(u, 0,width*height/4, reinterpret_cast<const jbyte *>(fu));

    jbyteArray v = jniEnv1->NewByteArray(width*height/4);
    jniEnv1->SetByteArrayRegion(v, 0,width*height/4, reinterpret_cast<const jbyte *>(fv));

    jniEnv1->CallVoidMethod(obj,jMid_renderYuv,width,height,y,u,v);

    jniEnv1->DeleteLocalRef(y);
    jniEnv1->DeleteLocalRef(u);
    jniEnv1->DeleteLocalRef(v);
    javaVm->DetachCurrentThread();

}

PlayCallback::~PlayCallback() {
    this->javaVm = nullptr;
    //释放全局引用
    jniEnv->DeleteGlobalRef(this->obj);
    this->obj = nullptr;
    jniEnv = nullptr;
}


