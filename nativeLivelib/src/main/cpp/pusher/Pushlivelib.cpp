#include <jni.h>
#include <string>
#include "PushClient.h"
extern "C"{
#include <libavformat/avformat.h>
}

PushClient *pushClient = nullptr;
JavaVM *javaVm = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_skyward_nativelivelib_PushLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF("sdsdsd");
}

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
Java_com_skyward_nativelivelib_PushLib_pushInit(JNIEnv *env, jobject thiz,
                                                jboolean is_media_codec) {
    if(!pushClient){
        LOGI("myLog ------ pushInit -------");
        pushClient = new PushClient();
        pushClient->init(javaVm,env,is_media_codec,thiz);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_startPush(JNIEnv *env, jobject thiz, jstring push_url) {

    if(pushClient && pushClient->isReadyPushing()){
        return;
    }
    LOGI("myLog pushClient->isReadyPushing() %d",pushClient->isReadyPushing());
    const char *path = env->GetStringUTFChars(push_url,NULL);
//    //开始推流
    pushClient->start(path);
    env->ReleaseStringUTFChars(push_url,path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_stopPush(JNIEnv *env, jobject thiz) {

    if(pushClient){
        LOGI("myLog  ---- PushLib_stopPush -----");
        pushClient->stop();
    }


}


extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_release(JNIEnv *env, jobject thiz) {
    if(pushClient){
        pushClient->release();
        delete pushClient;
        pushClient = nullptr;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_pushMediaCodecEncodeData(JNIEnv *env, jobject thiz,
                                                                jbyteArray data, jint len,
                                                                jlong tms, jint type) {

    jbyte *encodeData = env->GetByteArrayElements(data, NULL);
    switch (type) {
        //视频
        case  0:
            if(pushClient){
//                LOGI("myLog --- pushMediaCodecEncodeData--- lend: %d",env->GetArrayLength(data));
                pushClient->videoChannel->sendH264(type, reinterpret_cast<uint8_t *>(encodeData), env->GetArrayLength(data), tms);
            }
            break;
        //1 音频头，2 音频
        case 1:
        default:
            //默认先填双声道
            pushClient->audioChannel->pushAAC(reinterpret_cast<uint8_t *>(encodeData), len, tms,type);
            break;
    }

    env->ReleaseByteArrayElements(data,encodeData,0);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_initVideoCodec(JNIEnv *env, jobject thiz, jint width,
                                                      jint height, jint fps, jint bitrate) {
    if(pushClient){
        pushClient->videoChannel->openVideoCodec(width,height,fps,bitrate);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_initAudioCodec(JNIEnv *env, jobject thiz, jint sample_rate,
                                                      jint channel) {
    if(pushClient){
        pushClient->audioChannel->openAudioCodec(sample_rate,channel);
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_skyward_nativelivelib_PushLib_getInputSamples(JNIEnv *env, jobject thiz) {
    if(pushClient){
      return  pushClient->audioChannel->getInputSamples();
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_skyward_nativelivelib_PushLib_pushYuvAndPcmData(JNIEnv *env, jobject thiz,
                                                         jbyteArray data_, jint len,
                                                         jint type) {
    jbyte *data = env->GetByteArrayElements(data_,0);
    switch (type) {
        //视频
        case  0:
            if(pushClient){
//                LOGI("myLog ---- data: %d", len);
                pushClient->videoChannel->encodeData(data);
            }
            break;
        case 2:
            if(pushClient){
                pushClient->audioChannel->encodeData(reinterpret_cast<int32_t *>(data));
            }
            break;
    }
//    LOGI("myLog nav pushYuvAndPcmData type: %d",type);
    env->ReleaseByteArrayElements(data_,data,0);
}
