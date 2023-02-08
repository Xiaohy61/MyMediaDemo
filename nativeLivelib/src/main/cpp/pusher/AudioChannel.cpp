//
// Created by skyward on 2022/1/9.
//

#include "include/AudioChannel.h"

AudioChannel::AudioChannel() {

}

AudioChannel::AudioChannel(PushCallback *pushCallback, int isMediaCodec) {
    this->mPushCallback = pushCallback;
    this->isMediaCodec = isMediaCodec;
}

void AudioChannel::openAudioCodec(int sampleHz, int channel) {
    LOGI("myLog sampleHz:%d, channel: %d",sampleHz,channel);
    if(mAudioCodec){
        release();
    }
    this->mChannels = channel;
    //打开编码器 mInputSamples 样本数量，编码数据的个数  mMaxOutputBytes 最大可能输出数据，编码后的最大字节数
    mAudioCodec = faacEncOpen(sampleHz,mChannels,&mInputSamples,&mMaxOutputBytes);
    if(!mAudioCodec){
        if(mPushCallback){
            mPushCallback->OnError(THREAD_CHILD,FAAC_ENC_OPEN_ERROR);
        }
        LOGI("myLog faacEncOpen failure");
        return;
    }
    //保存编码后的数据缓冲区
    mOutputBuffer = static_cast<unsigned char *>(malloc(mMaxOutputBytes));
    //设置编码参数
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(mAudioCodec);
    configurationPtr->mpegVersion = MPEG4;
    configurationPtr->aacObjectType = LOW;
    configurationPtr->outputFormat = 0;
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;
    faacEncSetConfiguration(mAudioCodec,configurationPtr);
    isStart = true;
    LOGI("myLog --- openAudioCodec success ----");
}

int AudioChannel::getInputSamples() {
    return mInputSamples;
}

void AudioChannel::encodeData(int32_t *data) {
    if(!mAudioCodec || !isStart){
        LOGI("myLog ---audio encodeData---- isStart %d,mAudioCodec: %d",isStart,mAudioCodec);
        return;
    }
    //返回编码后的数据长度
    int byteLen = faacEncEncode(mAudioCodec, data, mInputSamples, mOutputBuffer, mMaxOutputBytes);
//    LOGI("myLog ---audio faac byteLen----:%d",byteLen);
    if(byteLen > 0){
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet,byteLen+2);
        //双声道
        packet->m_body[0] = 0xAF;
        if (mChannels == 1) {
            packet->m_body[0] = 0xAE;
        }
        packet->m_body[1] = 0x01;
        memcpy(&packet->m_body[2],mOutputBuffer,byteLen);
        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = byteLen+2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        //发送
        if (mAudioCallback){
            mAudioCallback(packet);
        }

//        LOGI("myLog ---audio faac encode----");
    }
}

void AudioChannel::pushAAC(uint8_t *data, int len, long timestamp,int type) {

//    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
//    RTMPPacket_Alloc(packet, len);
//    RTMPPacket_Reset(packet);
//    packet->m_nChannel = 0x05; //音频
//    memcpy(packet->m_body, data, len);
//    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
//    packet->m_hasAbsTimestamp = FALSE;
//    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
//    packet->m_nBodySize = len;
//    packet->m_nTimeStamp = timestamp;
    RTMPPacket *packet = createAudioPacket(data, len, type, timestamp);
    if (mAudioCallback){
        mAudioCallback(packet);
    }
}

/**
 * 音频头包数据
 * @return
 */
RTMPPacket *AudioChannel::getAudioTag() {
    if(!mAudioCodec || !isStart){
        LOGI("myLog ---getAudioTag--");
        return nullptr;
    }
    u_char *buf;
    u_long len;
    faacEncGetDecoderSpecificInfo(mAudioCodec, &buf, &len);
    int bodySize = 2 + len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);
    //双声道
    packet->m_body[0] = 0xAF;
    if (mChannels == 1) {
        packet->m_body[0] = 0xAE;
    }
    packet->m_body[1] = 0x00;
    //图片数据
    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = FALSE;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

RTMPPacket *AudioChannel::createAudioPacket(uint8_t *buf, const int len, int type, const long tms) {
    int bodySize = len +2;
    RTMPPacket *packet = (RTMPPacket*)malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet,bodySize);

    //双声道
    packet->m_body[0] = 0xAF;
    //单声道
    if (mChannels == 1) {
        packet->m_body[0] = 0xAE;
    }
    if(type == 1){
        //音频头
        packet->m_body[1] = 0x00;
        packet->m_nChannel = 0x11;
    } else{
        //音频数据
        packet->m_body[1] = 0x01;
        packet->m_nChannel = 0x05;
    }
    memcpy(&packet->m_body[2],buf,len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;

    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = FALSE;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}


void AudioChannel::release() {
    isStart = false;
    if (mAudioCallback) {
        mAudioCallback = nullptr;
    }
    //释放编码器
    if (mAudioCodec) {
        faacEncClose(mAudioCodec);
        mAudioCodec = nullptr;
    }
    if(mOutputBuffer){
        free(mOutputBuffer);
        mOutputBuffer = nullptr;
    }
}

void AudioChannel::setAudioCallback(AudioCallback audioCallback) {
    this->mAudioCallback = audioCallback;
}

void AudioChannel::startEncoder() {
    if (isMediaCodec)
        return;
    isStart = true;
}

void AudioChannel::restart() {
  isStart = true;
}

void AudioChannel::stop() {
   isStart = false;
}

void AudioChannel::setMediaCodec(int isMediaCodec) {
    this->isMediaCodec = isMediaCodec;
}

AudioChannel::~AudioChannel() {
    release();
}

