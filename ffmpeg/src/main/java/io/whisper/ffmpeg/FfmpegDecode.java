package io.whisper.ffmpeg;

public class FfmpegDecode {
	static {
		System.loadLibrary("ffmpeg_jni");
	}
	/*public native int getffmpegv();
	public native int decodeBegin(int width,int begin);
	public native int decodeEnd();  
	public native byte[] decode(byte[] inData, int dataSize);*/
	static public native int getffmpegv();
	static public native boolean DecodeInit(FFmpegDecodeHandler handler);
	static public native int Decoding(byte[] in,int datalen);
	static public native void DecodeRelease();
}
