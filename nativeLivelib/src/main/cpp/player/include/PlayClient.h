//
// Created by skyward on 2022/1/24.
//

#ifndef MYMEDIADEMO_PLAYCLIENT_H
#define MYMEDIADEMO_PLAYCLIENT_H
#include "PlayStatus.h"
#include "PlayCallback.h"
#include <pthread.h>
#include "PlayVideo.h"

extern "C"
{
#include <libavutil/time.h>
#include "libavformat/avformat.h"
};

class PlayClient {
public:
    pthread_t decodeThread;

    PlayClient(JavaVM *javaVm,JNIEnv *env,jobject obj,const char *url);
    void prepared();
    void decodeFFmpegThread();
    void start();
    void pause();
    void seek(int64_t second);
    void resume();
    void setSpeed(float speed);
    void setPitch(float pitch);
    void release();
    int getCodecContext(AVCodecParameters *parameters,AVCodecContext **avCodecContext);
    ~PlayClient();

private:
    PlayCallback *playCallback = nullptr;
    const char *url = nullptr;

    AVFormatContext *fmtCtx = nullptr;
    PlayVideo *video = nullptr;
    PlayStatus *playStatus = nullptr;
    int duration =0;
    pthread_mutex_t seek_mutex;
    pthread_mutex_t init_mutex;
    bool exit = false;
    int decodeFFmpegThreadResult = 0;
};


#endif //MYMEDIADEMO_PLAYCLIENT_H
