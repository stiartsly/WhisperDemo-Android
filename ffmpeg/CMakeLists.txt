# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(ffmpeg_library_DIR ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})

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

add_library( # Sets the name of the library.
             ffmpeg_jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/ffmpegDecode.c )

target_include_directories(ffmpeg_jni PRIVATE
                           include)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

find_library(z-lib z)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       ffmpeg_jni

                       avfilter
                       avformat
                       avcodec
                       swresample
                       swscale
                       avutil

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       ${z-lib})