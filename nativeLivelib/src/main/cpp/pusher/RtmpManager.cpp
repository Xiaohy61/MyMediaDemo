#include <__bit_reference>
//
// Created by skyward on 2022/1/9.
//

#include "include/RtmpManager.h"

RtmpManager *rtmpManager = nullptr;

//rtmp 链接回调
void *start(void *context){
    //start 是c语言函数，要调用c++就要拿到当前c++对象引用并转换成rtmpManager，这样就可以调用c++的方法了
    rtmpManager = static_cast<RtmpManager *>(context);
    rtmpManager->onConnect();
    return 0;
}

void releasePackets(RTMPPacket *&packet){
    if(packet){
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void callback(RTMPPacket *packet) {
    if(packet && rtmpManager){
//        LOGI("myLog -----callback--------");
        packet->m_nTimeStamp =RTMP_GetTime() - rtmpManager->mStartTime;
        rtmpManager->mPackets.push(packet);
    }
}

RtmpManager::RtmpManager(PushCallback *pushCallback, VideoChannel *videoChannel,
                         AudioChannel *audioChannel, int mediaCodec) {
    this->pushCallback = pushCallback;
    this->audioChannel = audioChannel;
    this->videoChannel = videoChannel;
    //设置监听
    this->videoChannel->setVideoCallback(callback);
    this->audioChannel->setAudioCallback(callback);
    //是否硬编
    this->isMediaCodec = mediaCodec;
    //保证队列没有脏数据
    setPacketReleaseCallback();

}

void RtmpManager::setPushUrl(const char *url) {
    //转换成c字符串，后面多一位
    char *pushUrl = new char[strlen(url)+1];
    strcpy(pushUrl,url);
    this->url = pushUrl;
    //创建线程 start是回调函数,里面初始化rtmp，并且开始链接
    pthread_create(&mPid,0,start,this);

}

//rtmp开始链接
void RtmpManager::onConnect() {
  if(pushCallback){
      pushCallback->onRtmpConnect(THREAD_CHILD);
  }
  //如果不为空就先关闭并且释放掉
  if(rtmp){
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
    rtmp = nullptr;
  }
  //申请空间
  this->rtmp = RTMP_Alloc();
  if(!rtmp){
      if(pushCallback){
          pushCallback->OnError(THREAD_CHILD,RTMP_INIT_ERROR);
      }
      return;
  }
  //初始化
  RTMP_Init(rtmp);
  //设置推流地址
  int ret = RTMP_SetupURL(rtmp,this->url);
    LOGI("myLog rtmp RTMP_SetupURL %s",this->url);
  if(!ret){
      if(pushCallback){
          pushCallback->OnError(THREAD_CHILD,RTMP_SET_URL_ERROR);
      }
      return;
  }
  LOGI("myLog rtmp init success");
  //设置超时时间
  rtmp->Link.timeout = 5;
  RTMP_EnableWrite(rtmp);
  ret = RTMP_Connect(rtmp,0);
  if(!ret){
      if(pushCallback){
          pushCallback->OnError(THREAD_CHILD,RTMP_CONNECT_ERROR);
      }
      LOGI("myLog rtmp connect failure %d",ret);
      return;
  }
    LOGI("myLog rtmp connect success");
  ret = RTMP_ConnectStream(rtmp,0);
  if(!ret){
      if(pushCallback){
          pushCallback->OnError(THREAD_CHILD,RTMP_CONNECT_ERROR);
      }
      return;
  }
//  记录开始时间
  mStartTime = RTMP_GetTime();
  //标记可以推流
  readyPushing = true;
  isStart = true;
  if(pushCallback){
      pushCallback->onRtmpSucceed(THREAD_CHILD);
  }
  //队列开始工作了
  mPackets.setWork(true);

//  LOGI("myLog  --- 队列开始工作了 isMediaCodec: %d",isMediaCodec);
  //不是硬编就走软编
  if(!isMediaCodec){
      this->videoChannel->startEncoder();
      this->audioChannel->startEncoder();
      //保证第一个数据包是音频
      if(audioChannel->getAudioTag()){
          LOGI("myLog ---getAudioTag--");
          callback(audioChannel->getAudioTag());
      }
  }
   //建立完成，开始推流
   onPush();
}

//rtmp 推流
void RtmpManager::onPush() {
  RTMPPacket *packet = nullptr;
    while (isStart){
        //从队列中取出数据
        mPackets.pop(packet);
        if(!readyPushing){
            releasePackets(packet);
        }
        //获取数据失败
        if(!packet){
            continue;
        }
        packet->m_nInfoField2 = rtmp->m_stream_id;
        //发送数据
        int ret = RTMP_SendPacket(rtmp,packet,1);
//        LOGI("myLog --- RTMP_SendPacket----");
        if(!ret){
            if(pushCallback){
                //回调java层
                pushCallback->OnError(THREAD_CHILD,RTMP_PUSHER_ERROR);
            }
            return;
        }
//        LOGI("myLog --- RTMP_SendPacket success----");
    }
    //跳出循环，说明不再推流就释放资源
    releasePackets(packet);
    release();
}


void RtmpManager::release() {
    isStart = false;
    readyPushing = false;
    if(rtmp){
        RTMP_DeleteStream(rtmp);
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = nullptr;
    }
    mPackets.clearQueue();

}

void RtmpManager::stop() {
    isStart = false;
   mPackets.setWork(false);
}

void RtmpManager::setMediaCodec(int mediaCodec) {
    this->isMediaCodec = mediaCodec;
}

RtmpManager::~RtmpManager() {
    if (pushCallback) {
        delete pushCallback;
        pushCallback = nullptr;
    }
}


void RtmpManager::restart() {

}

 void RtmpManager::setPacketReleaseCallback() {
     mPackets.setRtmpReleaseCallback(releasePackets);
}







