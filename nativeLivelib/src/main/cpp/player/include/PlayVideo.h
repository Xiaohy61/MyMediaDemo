//
// Created by skyward on 2022/1/24.
//

#ifndef MYMEDIADEMO_PLAYVIDEO_H
#define MYMEDIADEMO_PLAYVIDEO_H

#include "PlayStatus.h"
#include "PlayCallback.h"
#include "PlayDataQueue.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include "libavutil/time.h"
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
};

class PlayVideo {
public:
    int streamIndex = -1;
    AVCodecParameters *codecpar = nullptr;
    AVRational time_base;
    //默认休眠时间 40ms 0.04s 帧率25帧
    double defaultDelayTime=0.04;
    AVCodecContext *ctx = nullptr;
    PlayDataQueue *queue = nullptr;

    PlayVideo(PlayStatus *playStatus,PlayCallback *playCallback);
    void play();
    int convertYuv420p(AVFrame &frame);
    void callRenderYUV(AVFrame *frame);
    void decodeVideo();
    double getDelayTime(double diff);
    double getFrameDiffTime(AVFrame &avFrame);
    ~PlayVideo();
private:




    PlayStatus *playStatus = nullptr;
    PlayCallback *playCallback = nullptr;
    pthread_mutex_t codecMutex;
    pthread_t thread_play;
    double clock =0;
    //主要与音频的差值
    double delayTime =0;


};


#endif //MYMEDIADEMO_PLAYVIDEO_H
