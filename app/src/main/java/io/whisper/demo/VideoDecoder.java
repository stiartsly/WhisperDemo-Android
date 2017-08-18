package io.whisper.demo;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.whisper.ffmpeg.FFmpegDecodeHandler;
import io.whisper.rtp.UnPacket;
import io.whisper.ffmpeg.FfmpegDecode;

public class VideoDecoder implements FFmpegDecodeHandler {
    public static final boolean USE_FFMPEG = true;
    private static final String TAG = VideoDecoder.class.getSimpleName();

    private VideoDecodeThread videoDecodeThread;

    private MediaCodec mediaCodec;
    private long mCount;

    private View mVideoView;

    private Handler mainHandler;
    private class MainHandler extends Handler {

        public MainHandler(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mVideoView != null) {
                        Bitmap bitmap = (Bitmap) msg.obj;
                        ((ImageView)mVideoView).setImageBitmap(bitmap);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void configure(View videoView, int width, int height)
    {
        if (USE_FFMPEG) {
            mVideoView = videoView;
        }
        else {
            try {
                if (mediaCodec != null) {
                    mediaCodec.reset();
                } else {
                    mediaCodec = MediaCodec.createDecoderByType("Video/AVC");
                }

                Surface surface = ((SurfaceView) videoView).getHolder().getSurface();
                Log.d("Surface", "surface = " + surface);

                MediaFormat mediaFormat = MediaFormat.createVideoFormat("Video/AVC", width, height);
                mediaCodec.configure(mediaFormat, surface, null, 0);

                mCount = 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if (videoDecodeThread == null) {
            videoDecodeThread = new VideoDecodeThread();
            videoDecodeThread.start();
        }

        if (USE_FFMPEG) {
            mainHandler = new MainHandler();
        }
        else {;
            mediaCodec.start();
        }
    }

    public  void stop() {
        if (videoDecodeThread != null) {
            videoDecodeThread.exit();
            videoDecodeThread = null;
        }

        if (USE_FFMPEG) {
            mVideoView = null;
            mainHandler = null;
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    public void decode(byte[] bytes) {
        if (videoDecodeThread != null) {
            Message msg = new Message();
            msg.what = 0;
            msg.obj = bytes;
            videoDecodeThread.getHandler().sendMessage(msg);
        }
    }

    private class VideoDecodeThread extends Thread {
        private UnPacket unPacket;
        private Handler handler;
        private Looper looper;

        Handler getHandler() {
            return handler;
        }

        @Override
        public void run() {
            try {
                if (USE_FFMPEG) {
                    if (!FfmpegDecode.DecodeInit(VideoDecoder.this)) {
                        Log.e("FfmpegDecode", "FfmpegDecode init failed");
                        return;
                    }
                }

                if (unPacket == null) {
                    unPacket = new UnPacket();
                }

                Looper.prepare();
                looper = Looper.myLooper();
                handler = new ThreadHandler();
                Looper.loop();

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (USE_FFMPEG) {
                FfmpegDecode.DecodeRelease();
            }
        }

        public void exit() {
            if (looper != null) {
                looper.quit();
                looper = null;
            }
        }

        class ThreadHandler extends Handler {
            public ThreadHandler() {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);

                if (msg.what == 0) {
                    byte[] bytes = (byte[]) msg.obj;
                    byte[] out = unPacket.unPacket(bytes, bytes.length);
                    if (out != null) {
                        if (USE_FFMPEG) {
                            ffmpegDecode(out);
                        }
                        else {
                            hardwareDecode(out);
                        }
                    }
                    else {
                        Log.w(TAG, "unPacket return null");
                    }
                }
            }
        }
    }

    private void ffmpegDecode(byte[] bytes) {
        int result = FfmpegDecode.Decoding(bytes, bytes.length);
        if (result < 0) {
            Log.w(TAG, "Decoding return " + result);
        }
    }

    @Override
    public void onVideoImage(int width, int height, byte[] data) {
        if (mainHandler == null) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(buffer);
        buffer.rewind();

        Message message = mainHandler.obtainMessage(0);
        message.obj = bitmap;
        message.sendToTarget();
    }

    private void hardwareDecode(byte[] bytes) {
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(bytes, 0, bytes.length);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytes.length, mCount, 0);
            mCount++;
        } else {
            Log.e(TAG, "inputBufferIndex = " + inputBufferIndex);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        Log.d(TAG, "outputBufferIndex = " + outputBufferIndex);
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            Log.d(TAG, "outputBufferIndex = " + outputBufferIndex);
        }
    }
}
