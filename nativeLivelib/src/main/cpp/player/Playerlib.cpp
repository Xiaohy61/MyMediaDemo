//
// Created by skyward on 2022/1/25.
//
#include <jni.h>
#include <string>
#include "PlayClient.h"
JavaVM *javaVm = nullptr;
PlayClient *playClient = nullptr;


extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = -1;
    javaVm = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {

        return result;
    }
    return JNI_VERSION_1_4;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PlayLib_setUrl(JNIEnv *env, jobject thiz, jstring url) {
   const char *playUrl = env->GetStringUTFChars(url,0);
    if(!playClient){
        playClient = new PlayClient(javaVm,env,thiz,playUrl);
    }
    if(playClient){
        playClient->prepared();
    }
   env->ReleaseStringUTFChars(url,playUrl);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PlayLib_startPlay(JNIEnv *env, jobject thiz) {
    if(playClient){
        playClient->start();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PlayLib_pause(JNIEnv *env, jobject thiz) {
    if(playClient){
        playClient->pause();
    }
}