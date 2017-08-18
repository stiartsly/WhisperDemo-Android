package io.whisper.ffmpeg;

public interface FFmpegDecodeHandler {
    void onVideoImage(int width, int height, byte[] data);
}
