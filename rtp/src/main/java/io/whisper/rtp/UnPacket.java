package io.whisper.rtp;

public class UnPacket {
	static {
		System.loadLibrary("rtp");
	}
	public native byte[] unPacket(byte[] inData, int length);

}
