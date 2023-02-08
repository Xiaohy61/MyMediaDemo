//
// Created by skyward on 2022/1/24.
//

#include "include/PlayVideo.h"

PlayVideo::PlayVideo(PlayStatus *playStatus, PlayCallback *playCallback) {
    this->playStatus = playStatus;
    this->playCallback = playCallback;
    queue = new PlayDataQueue(playStatus);
    pthread_mutex_init(&codecMutex, NULL);
}

void *playVideo(void *context) {
    //转为c++对象
    PlayVideo *video = static_cast<PlayVideo *>(context);
    video->decodeVideo();
}

void PlayVideo::play() {
    //子线程播放解码
    pthread_create(&thread_play, NULL, playVideo, this);
}


void PlayVideo::decodeVideo() {
    while (playStatus != NULL && !playStatus->exit) {
        //seek
        if (playStatus->seek) {
            av_usleep(1000 * 100);
            continue;
        }
        if (playStatus->pause) {
            av_usleep(1000 * 100);
            continue;
        }
        if (queue->getQueueSize() == 0) {
            if (!playStatus->load) {
                playStatus->load = true;
                playCallback->onCallLoad(THREAD_CHILD, true);
                av_usleep(1000 * 100);
                continue;
            }
        }

        AVPacket *avPacket = av_packet_alloc();
        if (queue->getAVPacket(avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = nullptr;
            continue;
        }
        //多线程环境要加锁
        pthread_mutex_lock(&codecMutex);
        //发送packet数据包到解码器
        if (avcodec_send_packet(ctx, avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = nullptr;
            pthread_mutex_unlock(&codecMutex);
            continue;
        }

        AVFrame *avFrame = av_frame_alloc();
        //接收解码后的frame数据
        if (avcodec_receive_frame(ctx, avFrame) != 0) {
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = nullptr;
            av_packet_free(&avPacket);
            avPacket = nullptr;
            pthread_mutex_unlock(&codecMutex);
            continue;
        }
        if (avFrame->format == AV_PIX_FMT_YUV420P) {
            double diff = getFrameDiffTime(*avFrame);
            av_usleep(getDelayTime(diff) * 1000000);
            callRenderYUV(avFrame);
//            LOGI("myLog 当前是yuv420格式");
        } else {
            LOGI("myLog 当前不是yuv420格式");
            //不是yuv420p格式要重新转换
            if(convertYuv420p(*avFrame) != 0){
                continue;
            }
        }
        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = nullptr;
        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = nullptr;
        pthread_mutex_unlock(&codecMutex);
    }
    pthread_exit(&thread_play);
}

int PlayVideo::convertYuv420p(AVFrame &frame) {
    int ret  = 0;
    AVFrame *frameYuv420p = av_frame_alloc();
    //一帧数据大小
    int num = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, ctx->width, ctx->height, 1);
    uint8_t *buffer = static_cast<uint8_t *>(av_malloc(num * sizeof(uint8_t)));
    av_image_fill_arrays(frameYuv420p->data, frameYuv420p->linesize, buffer, AV_PIX_FMT_YUV420P,
                         ctx->width, ctx->height, 1);
    SwsContext *sws_ctx = sws_getContext(
            ctx->width, ctx->height, ctx->pix_fmt, ctx->width, ctx->height, AV_PIX_FMT_YUV420P,
            SWS_BICUBIC, NULL, NULL, NULL);
    if(!sws_ctx){
        av_frame_free(&frameYuv420p);
        av_free(frameYuv420p);
        av_free(buffer);
        pthread_mutex_unlock(&codecMutex);
        ret = -1;
    }
    sws_scale(sws_ctx,frame.data,frame.linesize,0,frame.height,frameYuv420p->data,frameYuv420p->linesize);

    callRenderYUV(frameYuv420p);

    av_frame_free(&frameYuv420p);
    av_free(frameYuv420p);
    av_free(buffer);
    sws_freeContext(sws_ctx);
    return ret;
}

void PlayVideo::callRenderYUV(AVFrame *frame) {
    if (playCallback) {
        playCallback->onCallRenderYUV(
                ctx->width, ctx->height,
                frame->data[0], frame->data[1], frame->data[2]
        );
    }
}


double PlayVideo::getDelayTime(double diff) {
    //音频视频两者 差异在3ms以内的 两者情况音频超视频 或者视频超音频
    //音频超视频 3ms的情况下
    if (diff > 0.003) {
        delayTime = delayTime * 2 / 3;
        if (delayTime < defaultDelayTime / 2) {
            //用户有所察觉
            delayTime = defaultDelayTime * 2 / 3;
        } else if (delayTime > defaultDelayTime * 2) {
            delayTime = defaultDelayTime * 2;
        }
    } else if (diff < -0.003) {
        delayTime = delayTime * 3 / 2;
        if (delayTime < defaultDelayTime / 2) {
            delayTime = defaultDelayTime * 2 / 3;
        } else if (delayTime > defaultDelayTime * 2) {
            delayTime = defaultDelayTime * 2;
        }
    }

    if (diff >= 0.5) {
        delayTime = 0;
    } else if (diff <= -0.5) {
        delayTime = defaultDelayTime * 2;
    }
    //音频太快，视频赶不上 视频队列直接清空，解析最新的数据
    if (diff >= 10) {
        queue->clearAVPacket();
        delayTime = defaultDelayTime;
    }
    //视频太快了，音频赶不上
    if (diff <= -10) {
        //音频队列清空
    }

    if (diff <= -10) {

    }

    return delayTime;
}

double PlayVideo::getFrameDiffTime(AVFrame &frame) {
    //先获取视频时间戳
    double pts = frame.best_effort_timestamp;
    if (pts == AV_NOPTS_VALUE) {
        pts = 0;
    }
    pts *= av_q2d(time_base);
    if (pts > 0) {
        clock = pts;
    }
//    double diff =
    return 0;
}

PlayVideo::~PlayVideo() {

}






