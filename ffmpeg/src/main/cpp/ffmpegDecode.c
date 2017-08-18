#include <string.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <libavcodec/avcodec.h>
#define  LOG_TAG    "ffmpegDecode"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

jobject jhandler = NULL;
AVCodecContext * pCodecCtx = NULL;
AVFrame * pFrame = NULL;
AVFrame * pFrameRGB = NULL;
struct SwsContext *img_convert_ctx = NULL;
uint8_t *out_buffer = NULL;
int out_size;

int callHandlerMethod(JNIEnv *env, jobject jobj, jstring name, jstring sig, ...)
{
    jclass jcls = (*env)->GetObjectClass(env, jobj);
    if (NULL == jcls) {
        return -1;;
    }
    jmethodID methodId = (*env)->GetMethodID(env, jcls, name, sig);
    if (methodId == NULL) {
        (*env)->DeleteLocalRef(env, jcls);
        return -1;
    }

    va_list args;
    va_start(args, sig);
    (*env)->CallVoidMethodV(env, jobj, methodId, args);
    va_end(args);
    (*env)->DeleteLocalRef(env, jcls);
    return 0;
}

JNIEXPORT jint JNICALL Java_io_whisper_ffmpeg_FfmpegDecode_getffmpegv(
		JNIEnv * env, jclass obj) {
	LOGI("getffmpegv");
	return avformat_version();
}

JNIEXPORT jboolean JNICALL Java_io_whisper_ffmpeg_FfmpegDecode_DecodeInit(
		JNIEnv * env, jclass obj, jobject handler) {
	LOGI("Decode_init");

	avcodec_register_all();
	//av_register_all();
	//avcodec_init();

    AVCodec *pCodec = avcodec_find_decoder(AV_CODEC_ID_H264);
	if (NULL == pCodec) {
        return JNI_FALSE;
    }

    pCodecCtx = avcodec_alloc_context3(pCodec);
    if (NULL == pCodecCtx) {
        return JNI_FALSE;
    }

    if (avcodec_open2(pCodecCtx, pCodec, NULL) != 0) {
        return JNI_FALSE;
    }

    pFrame = av_frame_alloc();
    if (NULL == pFrame) {
        return JNI_FALSE;
    }

    jhandler = (*env)->NewGlobalRef(env, handler);
    return jhandler != NULL;
}

JNIEXPORT jint JNICALL Java_io_whisper_ffmpeg_FfmpegDecode_Decoding(
		JNIEnv * env, jclass obj, const jbyteArray pSrcData, const jint DataLen) {
    if (pCodecCtx == NULL || pFrame == NULL) {
        return JNI_FALSE;
    }

	int frameFinished;
	int consumed_bytes;
	jbyte * Buf = (jbyte*)(*env)->GetByteArrayElements(env, pSrcData, 0);

    AVPacket packet;
    av_new_packet(&packet, DataLen);
    memcpy(packet.data, Buf, DataLen);
    consumed_bytes = avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
    av_free_packet(&packet);

	if (consumed_bytes >= 0 && frameFinished) {
        if (pFrameRGB == NULL) {
            pFrameRGB = av_frame_alloc();
            pFrameRGB->format = AV_PIX_FMT_RGB565;
            pFrameRGB->width = pFrame->width;
            pFrameRGB->height = pFrame->height;

            out_size = av_image_get_buffer_size(pFrameRGB->format,
                                                pFrameRGB->width,
                                                pFrameRGB->height,
                                                1);

            out_buffer = (uint8_t *)av_malloc(sizeof(uint8_t) * out_size);

            av_image_fill_arrays(pFrameRGB->data,
                                 pFrameRGB->linesize,
                                 out_buffer,
                                 pFrameRGB->format,
                                 pFrameRGB->width,
                                 pFrameRGB->height,
                                 1);
        }

        if (img_convert_ctx == NULL) {
            img_convert_ctx = sws_getContext(pFrame->width,
                                             pFrame->height,
                                             AV_PIX_FMT_YUV420P,// pCodecCtx->pix_fmt,
                                             pFrameRGB->width,
                                             pFrameRGB->height,
                                             pFrameRGB->format,
                                             SWS_BICUBIC,
                                             NULL,
                                             NULL,
                                             NULL);
        }

        if (img_convert_ctx != NULL) {
            sws_scale(img_convert_ctx,
                      pFrame->data,
                      pFrame->linesize,
                      0,
                      pFrame->height,
                      pFrameRGB->data,
                      pFrameRGB->linesize);

            jbyteArray jdata = (*env)->NewByteArray(env, out_size);
            (*env)->SetByteArrayRegion(env, jdata, 0, out_size, out_buffer);
            callHandlerMethod(env, jhandler, "onVideoImage", "(II[B)V",
                              pFrameRGB->width, pFrameRGB->height, jdata);
            (*env)->DeleteLocalRef(env, jdata);
        }
	}

	(*env)->ReleaseByteArrayElements(env, pSrcData, Buf, 0);
	return consumed_bytes;
}

JNIEXPORT void JNICALL Java_io_whisper_ffmpeg_FfmpegDecode_DecodeRelease(
		JNIEnv * env, jclass obj) {
    if (NULL != pFrame) {
        av_frame_free(&pFrame);
        pFrame = NULL;
    }

    if (NULL != out_buffer) {
        av_free(out_buffer);
        out_buffer = NULL;
    }

    if (NULL != pFrameRGB) {
        av_frame_free(&pFrameRGB);
        pFrame = NULL;
    }

    if (NULL != img_convert_ctx) {
        sws_freeContext(img_convert_ctx);
        img_convert_ctx = NULL;
    }

    if (NULL != pCodecCtx) {
        avcodec_close(pCodecCtx);
	    avcodec_free_context(&pCodecCtx);
        pCodecCtx = NULL;
    }

    if (NULL != jhandler) {
        (*env)->DeleteGlobalRef(env, jhandler);
        jhandler = NULL;
    }
}
