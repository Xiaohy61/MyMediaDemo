//
// Created by skyward on 2022/1/9.
//

#include "include/VideoChannel.h"
void *_startEncoder(void *pVoid){
    //c环境，获取c++上下文，转换拿到c++对象，转而调用c++的方法去处理
    VideoChannel *videoChannel = static_cast<VideoChannel *>(pVoid);
    videoChannel->onEncoder();
    return 0;
}

VideoChannel::VideoChannel() {

}

VideoChannel::VideoChannel(PushCallback *pushCallback, int isMediaCodec) {
    //初始化互斥锁
    this->mPushCallback = pushCallback;
    this->isMediaCodec = isMediaCodec;
}


void VideoChannel::setVideoCallback(VideoEncoderCallback videoCallback) {
    this->mVideoCallback = videoCallback;
}

void VideoChannel::openVideoCodec(int width, int height, int fps, int bit) {

    this->mWidth = width;
    this->mHeight = height;
    this->mFps = fps;
    this->mBit = bit;
    this->mY_Size = width*height;
    this->mUV_Size = mY_Size/4;
    //编码器存在就先释放
    if(mVideoCodec || pic_in){
        if(mVideoCodec){
            x264_encoder_close(mVideoCodec);
            mVideoCodec = nullptr;
        }
        if (pic_in) {
            x264_picture_clean(pic_in);
            pic_in = nullptr;
        }
    }
    //x264 编码器参数
    x264_param_t  param;

    //x264_preset_names[0] = ultrafast 最快 x264_tune_names[7] = zerolatency 无延迟
    x264_param_default_preset(&param,x264_preset_names[0],x264_tune_names[7]);
    //编码等级
    param.i_level_idc = 32;
    //输入数据格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //无b帧
    param.i_bframe = 0;
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_rc_method = X264_RC_ABR;
    //码率 单位kbps
    param.rc.i_bitrate =mBit;
    //瞬时最大码率
    param.rc.i_vbv_max_bitrate = mBit*1.2;
    //设置了i_vbv_max_bitrate必须设置此参数，码率控制区大小,单位kbps
    param.rc.i_vbv_buffer_size = mBit;

    //帧率
    param.i_fps_num = fps;
    param.i_fps_den =1;
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
    //用fps而不是时间戳来计算帧距离
    param.b_vfr_input =0;
    //帧距离(关键帧)  2s一个关键帧
    param.i_keyint_max = fps*2;
    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers =1;
    //多线程
    param.i_threads =1;

    x264_param_apply_profile(&param,"baseline");
    //打开编码器
    mVideoCodec = x264_encoder_open(&param);
    pic_in = new x264_picture_t ;
    x264_picture_alloc(pic_in,X264_CSP_I420,width,height);
    isStart = true;
    LOGI("myLog --- openVideoCodec success ---- fps:  %d",fps);

}


void VideoChannel::encodeData(int8_t *data) {
    if(isStart){
        mVideoPackets.push(data);
    }

}


void VideoChannel::startEncoder() {
if(isMediaCodec){
    LOGI("myLog 现在是硬编");
    return;
}
    LOGI("myLog ---现在走软编----");
mVideoPackets.setWork(true);
isStart = true;
pthread_create(&mPid,0,_startEncoder, this);
}

void VideoChannel::onEncoder() {
    while (isStart){
        if(!mVideoCodec){
            continue;
        }
        int8_t *data = nullptr;
        mVideoPackets.pop(data);
        if(!data){
            LOGI("myLog 获取yuv数据为空");
            continue;
        }
        //copy Y数据
        memcpy(this->pic_in->img.plane[0],data,mY_Size);
        //获取uv数据  nv21: yyyyyyyyvuvuvu 偶数位v 奇数位u
        for (int i =0;i < mUV_Size;i++){
            //获取u数据 奇数位
            *(pic_in->img.plane[1]+i) = *(data+mY_Size+i*2+1);
            //拿到v数据 偶数位
            *(pic_in->img.plane[2]+i) =*(data+mY_Size+i*2);
        }
        //编码出来的数据
        x264_nal_t *pp_nal;
        //编码出来的帧数量
        int pi_nal =0;
        //编码出的参数 类似Android的mediacodec的BufferInfo
        x264_picture_t pic_out;
        //执行编码
        int ret = x264_encoder_encode(mVideoCodec,&pp_nal,&pi_nal,pic_in,&pic_out);
        if(!ret){
            LOGI("myLog 编码失败");
            continue;
        }

        uint8_t sps[100];
        uint8_t pps[100];
        int sps_len,pps_len;
        for (int i = 0; i < pi_nal; ++i) {
            if(pp_nal[i].i_type == NAL_SPS){
                //排除掉 h264的间隔 00 00 00 01
                sps_len = pp_nal[i].i_payload - 4;
                memcpy(sps,pp_nal[i].p_payload+4,sps_len);
            } else if(pp_nal[i].i_type == NAL_PPS){
                pps_len = pp_nal[i].i_payload - 4;
                memcpy(pps,pp_nal[i].p_payload+4,pps_len);
                //发送sps和pps
                sendSpsPps(sps,pps,sps_len,pps_len);
            } else{
                //关键帧或者非关键帧
                sendX264Frame(pp_nal[i].i_type,pp_nal[i].p_payload,pp_nal[i].i_payload,0);
            }
        }
    }
}

void VideoChannel::restart() {
    isStart = true;
}

void VideoChannel::stop() {
    isStart = false;
    mVideoPackets.setWork(false);
    LOGI("myLog VideoChannel::stop()");
}


void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    int bodySize = 13 + sps_len + 3 + pps_len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //CompositionTime
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格 profile 如baseline、main、 high
    packet->m_body[i++] = sps[1];
    //profile_compatibility 兼容性
    packet->m_body[i++] = sps[2];
    //profile level
    packet->m_body[i++] = sps[3];
    //固定格式
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    //高八位
    packet->m_body[i++] = (sps_len >> 8) & 0xFF;
//   低八位
    packet->m_body[i++] = sps_len & 0xff;
    //拷贝sps的内容
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps 标识
    packet->m_body[i++] = 0x01;
    //pps length
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = pps_len & 0xff;
    // 拷贝pps内容
    memcpy(&packet->m_body[i], pps, pps_len);

    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
//    视频 04
    packet->m_nChannel = 0x04;
    //sps 和pps 没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    if (mVideoCallback && isStart) {
        LOGI("myLog send sps pps");
        mVideoCallback(packet);
    }

}

void VideoChannel::sendX264Frame(int type, uint8_t *payload, int i_payload, long i) {
    //去掉 00 00 00 01 (有4位)/ 00 00 01 (有三位)
    if(payload[2] == 0x00){
        i_payload -= 4;
        payload += 4;
    } else{
        i_payload -= 3;
        payload += 3;
    }
    int bodySize = 9 + i_payload;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet,bodySize);

    packet->m_body[0] = 0x27;
    if(type == NAL_SLICE_IDR){
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节
    packet->m_body[5] = (i_payload >> 24) & 0xff;
    packet->m_body[6] = (i_payload >> 16) & 0xff;
    packet->m_body[7] = (i_payload >> 8) & 0xff;
    packet->m_body[8] = (i_payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9], payload, i_payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x05;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    if (mVideoCallback && isStart)
        mVideoCallback(packet);
}


void VideoChannel::sendH264(int type, uint8_t *data, int len,long tms) {
//    LOGI("myLog -----sendH264-----");
    isStart = true;
    //sps 的标识 0x67
    if (data[4] == 0x67) {
        getH264SpsPpsInfo(data, len);
    }
    //I帧标识 0x65
    if (data[4] == 0x65) {
        sendSpsPps(sps, pps, sps_len, pps_len);
    }

    sendMediaCodecEncodeFrame(data,len,tms);

}

void VideoChannel::setMediaCodec(int mediacodec) {
    this->isMediaCodec = mediacodec;
}

void VideoChannel::release() {
    isStart = false;
    if (mVideoCallback) {
        mVideoCallback = nullptr;
    }
    if (mVideoCodec) {
        x264_encoder_close(mVideoCodec);
        mVideoCodec = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        pic_in = nullptr;
    }
    mVideoPackets.setWork(false);
    mVideoPackets.clearQueue();

}

VideoChannel::~VideoChannel() {
    release();
}

//获取硬编的sps和pps信息
void VideoChannel::getH264SpsPpsInfo(uint8_t *data, int len) {
    for (int i = 0; i < len; i++) {
//        防止越界
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                if (data[i + 4] == 0x68) {
                    sps_len = i - 4;
//                    new一个数组
                    this->sps = static_cast<uint8_t *>(malloc(sps_len));
//                    sps解析出来了
                    memcpy(this->sps, data + 4, sps_len);

//                    解析pps
                    pps_len = len - (4 + sps_len) - 4;
//                    实例化PPS 的数组
                    this->pps = static_cast<uint8_t *>(malloc(pps_len));
                    memcpy(pps, data + 4 + sps_len + 4, pps_len);
                    LOGI("myLog sps:%d pps:%d", sps_len, pps_len);
                    break;
                }
            }

        }
    }
}

void VideoChannel::sendMediaCodecEncodeFrame(uint8_t *data, int len, long tms) {
    data += 4;
    len -= 4;
    int body_size = len + 9;

    RTMPPacket *packet = (RTMPPacket *) (malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, body_size);
    RTMPPacket_Reset(packet);


    if (data[0] == 0x65) {
        packet->m_body[0] = 0x17;
//        LOGI("myLog 发送关键帧 data");
    } else{
        packet->m_body[0] = 0x27;
//        LOGI("myLog 发送非关键帧 data");
    }
//    固定的大小
    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    //长度
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;

    //数据
    memcpy(&packet->m_body[9], data, len);
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x05;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    mVideoCallback(packet);
}
