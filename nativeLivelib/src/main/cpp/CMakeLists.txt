# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.6.0)
project("nativelivelib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")


#设置库文件目录
set(jnilibs ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI})
set(ffmpeg ${CMAKE_SOURCE_DIR}/ffmpeg)
set(rtmp ${CMAKE_SOURCE_DIR}/rtmp)
set(x264 ${CMAKE_SOURCE_DIR}/x264)
set(faac ${CMAKE_SOURCE_DIR}/faac)
set(pusher ${CMAKE_SOURCE_DIR}/pusher)
set(player ${CMAKE_SOURCE_DIR}/player)


#  添加头文件目录
include_directories(
        ${ffmpeg}/include
        ${rtmp}/include
        ${x264}/include
        ${faac}/include
        ${CMAKE_SOURCE_DIR}/utils
        ${pusher}/include
        ${player}/include
        ${CMAKE_SOURCE_DIR}/yuv/libyuv}
        ${CMAKE_SOURCE_DIR}/yuv}

)



#-------添加ffmpeg相关的so库--------
add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${jnilibs}/libavutil.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${jnilibs}/libswresample.so)

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${jnilibs}/libavcodec.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${jnilibs}/libavfilter.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${jnilibs}/libswscale.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${jnilibs}/libavformat.so)

set(ffmpeg_lib
        avutil
        swresample
        avcodec
        avfilter
        swscale
        avformat
        )
#-------------------------------

#----------rtmp-----------
add_library(rtmp STATIC IMPORTED)
set_target_properties(rtmp PROPERTIES IMPORTED_LOCATION ${jnilibs}/librtmp.a)
#---------------------------------

#-------------x264---------------
add_library(x264 STATIC IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION ${jnilibs}/libx264.a)
#---------------------------------

#----------faac-----------
add_library(faac STATIC IMPORTED)
set_target_properties(faac PROPERTIES IMPORTED_LOCATION ${jnilibs}/libfaac.a)
#---------------------------------

#-------yuv------------------------
add_library(yuv SHARED IMPORTED)
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION  ${PROJECT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libyuv.so)
file(GLOB yuv_CPP ${CMAKE_SOURCE_DIR}/yuv/*.CPP)
add_library(
        yuvconver
        SHARED
        ${yuv_CPP})
#---------------------------------

message("-------------------")
message("CMAKE_CURRENT_SOURCE_DIR: ${CMAKE_CURRENT_SOURCE_DIR}")
message("jnilibs: ${jnilibs}")
message("FFMPEG ${FFMPEG}")
message("x264 ${x264}/include")
message("CMAKE_ANDROID_ARCH_ABI ${CMAKE_ANDROID_ARCH_ABI}")
message("CMAKE_SOURCE_DIR ${CMAKE_SOURCE_DIR}")
message("-------------------")


#批量添加自己编写的推流端 cpp 文件,不要把 *.h 加入进来了

file(GLOB push_CPP ${CMAKE_SOURCE_DIR}/pusher/*.CPP)
file(GLOB player_CPP ${CMAKE_SOURCE_DIR}/player/*.CPP)

add_library(
        pushlivelib
        SHARED
        ${push_CPP})
add_library(
        playerlib
        SHARED
        ${player_CPP})
#----------------------------------------------------------


find_library(
        log-lib
        log)

#推流端
target_link_libraries(
        pushlivelib
        ${ffmpeg_lib}
        rtmp
        x264
        faac
        yuv
        yuvconver
        ${log-lib})
#播放端
target_link_libraries(
        playerlib
        ${ffmpeg_lib}
        ${log-lib})