//
// Created by skyward on 2022/1/9.
//

#include "include/PushClient.h"

void PushClient::init(JavaVM *javaVm, JNIEnv *jniEnv, bool isMediaCodec, jobject obj) {
    PushCallback *pushCallback = new PushCallback(javaVm, jniEnv, obj);
    VideoChannel *videoChannel = new VideoChannel(pushCallback, isMediaCodec);
    AudioChannel *audioChannel = new AudioChannel(pushCallback, isMediaCodec);
    RtmpManager *rtmpManager = new RtmpManager(pushCallback, videoChannel, audioChannel,
                                               isMediaCodec);
    this->videoChannel = videoChannel;
    this->audioChannel = audioChannel;
    this->rtmpManager = rtmpManager;
}


int PushClient::isStart() {
    bool  mIsStart = false;
    if(rtmpManager){
        mIsStart = rtmpManager->isStart;
    }
    return mIsStart;
}

int PushClient::isReadyPushing() {
    bool  mReadyPushing = false;
    if(rtmpManager){
        mReadyPushing = rtmpManager->readyPushing;
    }
    return mReadyPushing;
}

void PushClient::release() {
    if (videoChannel) {
        videoChannel->release();
        delete videoChannel;
        videoChannel = nullptr;
    }
    if (audioChannel) {
        audioChannel->release();
        delete audioChannel;
        audioChannel = nullptr;
    }
    if (rtmpManager) {
        rtmpManager->release();
        delete rtmpManager;
        rtmpManager = nullptr;
    }

}

void PushClient::stop() {
    if (videoChannel) {
        videoChannel->stop();
    }
    if (audioChannel) {
        audioChannel->stop();
    }
    if(rtmpManager){
        rtmpManager->stop();
    }
}

void PushClient::start(const char *path) {
  rtmpManager->setPushUrl(path);
}

void PushClient::restartPush() {
 if(videoChannel){
     videoChannel->restart();
 }
 if(audioChannel){
     audioChannel->restart();
 }
 if(rtmpManager){
     rtmpManager->restart();
 }
}

void PushClient::setMediaCodec(int mediacodec) {
 if(videoChannel){
     videoChannel->setMediaCodec(mediacodec);
 }
 if(audioChannel){
     audioChannel->setMediaCodec(mediacodec);
 }
 if(rtmpManager){
     rtmpManager->setMediaCodec(mediacodec);
 }
}
