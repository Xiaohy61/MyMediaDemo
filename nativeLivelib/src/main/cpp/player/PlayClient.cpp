//
// Created by skyward on 2022/1/24.
//

#include "include/PlayClient.h"

PlayClient::PlayClient(JavaVM *javaVm, JNIEnv *env, jobject obj, const char *url) {
    if(!playStatus){
        playStatus = new PlayStatus;
    }
    if(!playCallback){
        playCallback = new PlayCallback(javaVm,env,obj);
    }
    this->url = url;
    pthread_mutex_init(&seek_mutex,NULL);
    pthread_mutex_init(&init_mutex,NULL);
}



void *decodeFFmpeg(void *context){
    PlayClient *playClient = static_cast<PlayClient *>(context);
    playClient->decodeFFmpegThread();
    pthread_exit(&playClient->decodeThread);
}

void PlayClient::prepared() {
    pthread_create(&decodeThread,NULL,decodeFFmpeg, this);
}

void PlayClient::decodeFFmpegThread() {
    LOGI("myLog 开始--decodeFFmpegThread----");
//    pthread_mutex_lock(&init_mutex);
//    avformat_network_init();
    fmtCtx = avformat_alloc_context();
    if(avformat_open_input(&fmtCtx,url,NULL,NULL) != 0){
        LOGI("myLog 打开播放url 失败 %s",url);
        decodeFFmpegThreadResult = FFMPEG_CAN_NOT_OPEN_URL;
        return;
    }
    fmtCtx->max_analyze_duration = 3 * AV_TIME_BASE;
    fmtCtx->probesize = 2048;
    LOGI("myLog ---avformat_open_input----");
    if(avformat_find_stream_info(fmtCtx,NULL) < 0){
        LOGI("myLog can not find streams from %s",url);
        decodeFFmpegThreadResult = FFMPEG_CAN_NOT_FIND_STREAMS;
        return;
    }
    LOGI("myLog ---avformat_find_stream_info----");
    for (int i = 0; i < fmtCtx->nb_streams; ++i) {

        if(fmtCtx->streams[i]->codecpar->codec_type ==AVMEDIA_TYPE_AUDIO){//得到音频流


        } else if(fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO){//找到视频流
            if(video == nullptr){
//                LOGI("myLog find video width: %d,height: %d",fmtCtx->streams[i]->codecpar->width,fmtCtx->streams[i]->codecpar->height);
                video = new PlayVideo(playStatus,playCallback);
                video->streamIndex = i;
                video->codecpar = fmtCtx->streams[i]->codecpar;
                video->time_base= fmtCtx->streams[i]->time_base;
                int num = fmtCtx->streams[i]->avg_frame_rate.num;
                int den = fmtCtx->streams[i]->avg_frame_rate.den;
                //假如 25帧的视频
                if(num != 0 && den != 0){
                    int fps = num / den;//[25 / 1]
                    video->defaultDelayTime = 1.0/fps;
                }
            }
        }
    }
    LOGI("myLog ---find_video_audio_stream_info----");
    if(video != nullptr){
        LOGI("myLog preview video width: %d,height: %d",0,0);
        getCodecContext(video->codecpar,&video->ctx);
        decodeFFmpegThreadResult = 1;
        LOGI("myLog load video width: %d,height: %d",video->ctx->width,video->ctx->height);
        playCallback->onCallVideoInfo(THREAD_CHILD,video->ctx->width,video->ctx->height);
    }
    if(playCallback){
        playCallback->onCallPrepared(THREAD_CHILD);

    }
//    pthread_mutex_unlock(&init_mutex);

}

void PlayClient::start() {

    while (true){
        if(decodeFFmpegThreadResult != 0){
            break;
        }
    }
    if(!video){
        return;
    }
  video->play();
  int count = 0;
    while (playStatus != nullptr && !playStatus->exit){
        if(playStatus->seek){
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        if(av_read_frame(fmtCtx,avPacket) == 0){
            if(avPacket->stream_index == video->streamIndex){
                count++;
//                LOGE("myLog 解码视频第 %d 帧", count);
                video->queue->putAVPacket(avPacket);
            } else{
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
        } else{
            av_packet_free(&avPacket);
            av_free(avPacket);
//            while (playStatus != nullptr && !playStatus->exit){
//
//            }
        }
    }
    LOGI("myLog ----解码完成-----");
}

void PlayClient::pause() {

}

void PlayClient::seek(int64_t second) {

}

void PlayClient::resume() {

}

void PlayClient::setSpeed(float speed) {

}

void PlayClient::setPitch(float pitch) {

}

void PlayClient::release() {

}

int PlayClient::getCodecContext(AVCodecParameters *codecpar, AVCodecContext **avCodecContext) {
    AVCodec *avCodec = avcodec_find_decoder(codecpar->codec_id);
    if(!avCodec){
        exit = true;
//        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    *avCodecContext = avcodec_alloc_context3(avCodec);
    if(avcodec_parameters_to_context(*avCodecContext,codecpar) < 0){
        exit = true;
//        pthread_mutex_unlock(&init_mutex);
        return  -1;
    }

    AVDictionary *pAVDictionary = nullptr;
    av_dict_set(&pAVDictionary, "buffer_size", "1024000", 0);
    av_dict_set(&pAVDictionary, "stimeout", "20000000", 0);
    av_dict_set(&pAVDictionary, "max_delay", "30000000", 0);
    av_dict_set(&pAVDictionary, "rtsp_transport", "tcp", 0);

    if(avcodec_open2(*avCodecContext,avCodec,&pAVDictionary) != 0){
        exit = true;
//        pthread_mutex_unlock(&init_mutex);
        return -1;
    }

    return 0;
}

PlayClient::~PlayClient() {
    pthread_mutex_destroy(&seek_mutex);
    pthread_mutex_destroy(&init_mutex);
}


