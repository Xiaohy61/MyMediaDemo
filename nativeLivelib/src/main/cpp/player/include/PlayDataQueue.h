//
// Created by skyward on 2022/1/24.
//

#ifndef MYMEDIADEMO_PLAYDATAQUEUE_H
#define MYMEDIADEMO_PLAYDATAQUEUE_H


#include <queue>
#include "PlayStatus.h"
#include <pthread.h>

extern "C"{
#include <libavcodec/avcodec.h>
};

class PlayDataQueue {
public:
    std::queue<AVPacket*> queuePacket;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;
    PlayStatus *mPlayStatus = nullptr;
public:
    PlayDataQueue(PlayStatus *playStatus);
    ~PlayDataQueue();
    int putAVPacket(AVPacket *packet);
    int getAVPacket(AVPacket *packet);
    int getQueueSize();
    void clearAVPacket();
};


#endif //MYMEDIADEMO_PLAYDATAQUEUE_H
