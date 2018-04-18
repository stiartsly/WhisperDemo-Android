cmake_minimum_required(VERSION 3.4.1)

set(ffmpeg_library_DIR ${CMAKE_SOURCE_DIR}/native-dist/libs/${ANDROID_ABI})
set(ffmpeg_include_DIR ${CMAKE_SOURCE_DIR}/native-dist/include)

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec    PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libavcodec.a)
add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter   PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libavfilter.a)
add_library(avformat SHARED IMPORTED)
set_target_properties(avformat   PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libavformat.a)
add_library(avutil SHARED IMPORTED)
set_target_properties(avutil     PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libavutil.a)
add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libswresample.a)
add_library(swscale SHARED IMPORTED)
set_target_properties(swscale    PROPERTIES IMPORTED_LOCATION ${ffmpeg_library_DIR}/libswscale.a)

add_library( ffmpeg_jni
             SHARED
             src/main/cpp/ffmpegDecode.c )

target_include_directories(ffmpeg_jni PRIVATE
                           ${ffmpeg_include_DIR})

find_library(log-lib log )
find_library(z-lib z)

target_link_libraries( ffmpeg_jni
                       avfilter
                       avformat
                       avcodec
                       swresample
                       swscale
                       avutil
                       ${log-lib}
                       ${z-lib})
