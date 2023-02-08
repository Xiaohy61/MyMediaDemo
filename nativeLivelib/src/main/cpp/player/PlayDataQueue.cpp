//
// Created by skyward on 2022/1/24.
//

#include "include/PlayDataQueue.h"

PlayDataQueue::PlayDataQueue(PlayStatus *playStatus) {
    this->mPlayStatus = playStatus;
    pthread_mutex_init(&mutexPacket,NULL);
    pthread_cond_init(&condPacket,NULL);
}

PlayDataQueue::~PlayDataQueue() {
    clearAVPacket();
}


int PlayDataQueue::putAVPacket(AVPacket *packet) {
    pthread_mutex_lock(&mutexPacket);
    queuePacket.push(packet);
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

int PlayDataQueue::getAVPacket(AVPacket *packet) {
    pthread_mutex_lock(&mutexPacket);
    while (mPlayStatus != NULL && !mPlayStatus->exit){
        if(queuePacket.size() > 0){
            AVPacket *avPacket = queuePacket.front();
            if(av_packet_ref(packet,avPacket) == 0){
                queuePacket.pop();
            }
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            break;
        } else{
            pthread_cond_wait(&condPacket,&mutexPacket);
        }

    }
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

int PlayDataQueue::getQueueSize() {
    int size = 0;
    pthread_mutex_lock(&mutexPacket);
    size = queuePacket.size();
    pthread_mutex_unlock(&mutexPacket);
    return size;
}

void PlayDataQueue::clearAVPacket() {
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
    while (!queuePacket.empty()){
        AVPacket *packet = queuePacket.front();
        queuePacket.pop();
        av_packet_free(&packet);
        av_free(packet);
        packet = NULL;
    }
}

