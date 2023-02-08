//
// Created by skyward on 2021/8/25.
//

#ifndef LOG_HPP
#define LOG_HPP

#include <android/log.h>

#define TAG "my_jni_log"
static bool debug = true;



#define LOGV(...) if (debug) __android_log_print(ANDROID_LOG_VERBOSE,TAG ,__VA_ARGS__)
#define LOGD(...) if (debug) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define LOGI(...) if (debug) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOGW(...) if (debug) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define LOGE(...) if (debug) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)

#endif //LOG_HPP

