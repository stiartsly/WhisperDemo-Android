package io.whisper.demo.device;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import java.util.HashMap;

import io.whisper.core.Whisper;
import io.whisper.demo.MainApp;

import io.whisper.core.FriendInfo;
import io.whisper.demo.VideoDecoder;
import io.whisper.exceptions.WhisperException;
import io.whisper.session.AbstractStreamHandler;
import io.whisper.session.IceTransportOptions;
import io.whisper.session.Manager;
import io.whisper.session.Session;
import io.whisper.session.SessionRequestCompleteHandler;
import io.whisper.session.Stream;
import io.whisper.session.StreamState;
import io.whisper.session.StreamType;
import io.whisper.session.TransportOptions;
import io.whisper.session.TransportType;

public class Device extends AbstractStreamHandler {
    private static final String TAG = Device.class.getSimpleName();

    private static final String turnServer  = "ws.iwhisper.io";
    private static final String turnUsername = "whisper";
    private static final String turnPassword = "io2016whisper";

	public FriendInfo deviceInfo;
	public boolean online;
    public HashMap<String, Object> status;

    private Session mSession;
    private Stream mStream;
    private String mSdp;
    private StreamState mState = StreamState.Closed;
    private boolean didSessionRequestComplte = false;

    private VideoDecoder mVideoDecoder;
    private boolean mRemotePlaying = false;

	public String getDeviceId() {
		return deviceInfo.getUserId();
	}

	public String getDeviceName() {
		String deviceName = deviceInfo.getLabel();
		if (deviceName == null || deviceName.length() == 0) {
			deviceName = deviceInfo.getName();
		}
		if (deviceName == null || deviceName.length() == 0) {
			deviceName = deviceInfo.getUserId();
		}

		return deviceName;
	}

	public boolean startVideo(View videoView) {
        try {
            if (mStream == null) {
                if (mSession == null) {
                    String target = getDeviceId() + "@" + getDeviceId();
                    IceTransportOptions opts = new IceTransportOptions();
                    opts.setStunHost(turnServer)
                            .setTurnHost(turnServer)
                            .setTurnUserName(turnUsername)
                            .setTurnPassword(turnPassword)
                            .setThreadModel(TransportOptions.SHARED_THREAD);

                    mSession = Manager.getInstance().newSession(target, opts);
                }

                mStream = mSession.addStream(StreamType.Application, 0, this);
                Log.i(TAG, "Added a stream, return id: " + mStream);
            }
            else if (mState == StreamState.TransportReady) {
                sendSessionRequest();
            }
            else if (mState == StreamState.Connected) {
                sendVideoCommand();
            }

            if (mVideoDecoder == null) {
                mVideoDecoder = new VideoDecoder();
            }

            mVideoDecoder.configure(videoView, videoView.getWidth(), videoView.getHeight());
            mVideoDecoder.start();
            return true;
        }
        catch (WhisperException e) {
            e.printStackTrace();
            return false;
        }
	}

	public void stopVideo() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "modify");
            jsonObject.put("camera", false);

            Whisper.getInstance().sendFriendMessage(getDeviceId(), jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
            mVideoDecoder = null;
        }

        if (mRemotePlaying) {
            disconnect();
        }
	}

    public void disconnect() {
        mRemotePlaying = false;
        mSdp = null;

        if (mSession != null) {
            if (mStream != null) {
                Stream stream = mStream;
                mStream = null;

                try {
                    mSession.removeStream(stream);
                }
                catch (WhisperException e) {
                    e.printStackTrace();
                }
            }

            mSession.close();
            mSession = null;
            mState = StreamState.Closed;
        }
    }

	public void onSessionRequest(String sdp) {
        mRemotePlaying = true;
        mSdp = sdp;

        try {
            if (mStream == null) {
                if (mSession == null) {
                    String target = getDeviceId() + "@" + getDeviceId();
                    IceTransportOptions opts = new IceTransportOptions();
                    opts.setStunHost(turnServer)
                            .setTurnHost(turnServer)
                            .setTurnUserName(turnUsername)
                            .setTurnPassword(turnPassword)
                            .setThreadModel(TransportOptions.SHARED_THREAD);

                    mSession = Manager.getInstance().newSession(target, opts);
                }

                mStream = mSession.addStream(StreamType.Application, 0, this);
                Log.i(TAG, "Added a stream, return id: " + mStream);
            }
            else if (mState == StreamState.Initialized) {
                mSession.replyRequest(0, null);
            }
        }
        catch (WhisperException e) {
            e.printStackTrace();
            connectFailed();
        }
    }

    private void sendSessionRequest() throws WhisperException {
        mSession.request(new SessionRequestCompleteHandler() {
            @Override
            public void onCompletion(Session session, int status, String reason, String sdp) {
                if (session != mSession || mState != StreamState.TransportReady) {
                    return;
                }

                if (status == 0) {
                    didSessionRequestComplte = true;
                    mSdp = sdp;
                }

                /*if (status == 0) {
                    try {
                        session.start(sdp);
                    } catch (WhisperException e) {
                        e.printStackTrace();
                        connectFailed();
                    }
                } else {
                    Log.e(TAG, "session invite response : " + reason);
                    connectFailed();
                }*/
            }
        });

        Log.d(TAG, " Send session request successfully");
    }

    private void sendVideoCommand() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "modify");
            jsonObject.put("camera", true);

            Whisper.getInstance().sendFriendMessage(getDeviceId(), jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectFailed() {
        HashMap<String, Object> status = new HashMap();
        status.put("type", "sync");
        status.put("camera", false);

        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
            mVideoDecoder = null;

            Intent intent = new Intent(DeviceManager.ACTION_DEVICE_STATUS_CHANGED);
            intent.putExtra("deviceId", getDeviceId());
            intent.putExtra("status", status);
            MainApp.getAppContext().sendBroadcast(intent);
        }

        if (mRemotePlaying) {
            mRemotePlaying = false;
            mSdp = null;

            JSONObject jsonObject = new JSONObject(status);

            try {
                Whisper.getInstance().sendFriendMessage(getDeviceId(), jsonObject.toString());
            } catch (WhisperException e) {
                e.printStackTrace();
            }
        }
    }

    public void onStateChanged(Stream stream, StreamState state) {
        Log.i(TAG, "Stream " + stream + " state changed : " + state.value());

        if (stream != mStream) {
            return;
        }

        mState = state;

        switch (state) {
            case Initialized:
                try {
                    if (mRemotePlaying == true && mSdp != null) {
                        mSession.replyRequest(0, null);
                    }
                    else {
                        sendSessionRequest();
                    }
                }
                catch (WhisperException e) {
                    e.printStackTrace();
                    connectFailed();
                }
                break;

            case TransportReady:
                if (!mRemotePlaying) {
                    try {
                        while (!didSessionRequestComplte)
                            Thread.sleep(500);
                        mSession.start(mSdp);
                    } catch (WhisperException e) {
                        e.printStackTrace();
                        connectFailed();
                    } catch (Exception e) {
                        e.printStackTrace();
                        connectFailed();
                    }
                } else {
                    try {
                        mSession.start(mSdp);
                    } catch (WhisperException e) {
                        e.printStackTrace();
                        connectFailed();
                    }
                }
                break;

            case Connecting:
                break;

            case Connected:
                if (mVideoDecoder != null) {
                    sendVideoCommand();
                }
                break;

            default:
                disconnect();
                break;
        }
    }

    public void onStreamData(Stream stream, byte[] data) {
        if (stream != mStream) {
            return;
        }

        try {
            Log.i(TAG, "onStreamData : " + data.length);
            mVideoDecoder.decode(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
