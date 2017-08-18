#include <android/log.h>
#include <string.h>
#include "CH264_RTP_UNPACK.h"
#include <jni.h>

#define logd(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, "JNI", fmt, ##args)
#define loge(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, "JNI", fmt, ##args)
static CH264_RTP_UNPACK* unpack = NULL;

extern "C" {
JNIEXPORT jbyteArray JNICALL Java_io_whisper_rtp_UnPacket_unPacket(
		JNIEnv *env, jobject obj, jbyteArray inData, jint length) {
	jbyte* buf = NULL;
	jbyteArray bytes = NULL;
	unsigned char* result = NULL;
	int initError;
	unsigned int outsize = 0;
	unsigned int mytimestamp = 0;
    if(NULL == unpack){
	 unpack =new CH264_RTP_UNPACK(initError);
    }
	buf = env->GetByteArrayElements(inData, 0);
	if (NULL == buf) {
		delete unpack;
		return NULL;
	}
	result = unpack->Parse_RTP_Packet((unsigned char*)buf, (short)length, &outsize,
			&mytimestamp);
    env->ReleaseByteArrayElements(inData, buf, 0);
	if (NULL != result) {
		bytes = env->NewByteArray(outsize);
		if(NULL == bytes){
			delete unpack;
			return NULL;
		}
		(env)->SetByteArrayRegion(bytes, 0, outsize, (jbyte*) result);
		return bytes;
	} else {
		return NULL;
	}
}
}
