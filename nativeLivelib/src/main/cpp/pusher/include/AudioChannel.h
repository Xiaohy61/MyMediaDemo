//
// Created by skyward on 2022/1/9.
//

#ifndef MYLIVE_AUDIOCHANNEL_H
#define MYLIVE_AUDIOCHANNEL_H

#include "PushCallback.h"
#include <sys/types.h>
#include <cstring>
#include "MyLog.hpp"
#include "safe_queue.h"
#include <faac.h>
extern "C"{
#include <rtmp.h>
}

#define FAAC_DEFAUTE_SAMPLE_RATE 44100
#define FAAC_DEFAUTE_SAMPLE_CHANNEL 1

class AudioChannel {
  typedef void (*AudioCallback)(RTMPPacket *packet);

public:
  AudioChannel();
  AudioChannel(PushCallback *pushCallback,int mediaCodec);
  void openAudioCodec(int sampleHz,int channel);
  int getInputSamples();
  void encodeData(int32_t *data);
  void pushAAC(uint8_t *data,int len,long timestamp,int type,int channel);
  RTMPPacket * createAudioPacket(uint8_t *buf,const int len,int type,const long  tms,int channel);
  RTMPPacket *getAudioTag();
  void release();
  void setAudioCallback(AudioCallback audioCallback);
  PushCallback *mPushCallback;
  void startEncoder();
  void restart();
  void stop();
  void setMediaCodec(int isMediaCodec);
  ~AudioChannel();

private:
    AudioCallback mAudioCallback;
    int mChannels =1;
    faacEncHandle mAudioCodec = 0;
    unsigned long mInputSamples = 0;
    unsigned long mMaxOutputBytes =0;
    unsigned char *mOutputBuffer = 0;
    int isStart = 0;
    int isMediaCodec = 0;
};


#endif //MYLIVE_AUDIOCHANNEL_H
