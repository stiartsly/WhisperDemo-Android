package io.whisper.demo.device;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.whisper.demo.MainApp;

import io.whisper.demo.R;
import io.whisper.core.*;
import io.whisper.session.Manager;
import io.whisper.exceptions.WhisperException;

public class DeviceManager implements WhisperHandler {

    public static final String ACTION_CONNECTION_STATUS_CHANGED =
            "io.whisper.demo.CONNECTION_STATUS_CHANGED";
    public static final String ACTION_SELF_INFO_CHANGED =
            "io.whisper.demo.SELF_INFO_CHANGED";
    public static final String ACTION_DEVICE_INFO_CHANGED =
            "io.whisper.demo.DEVICE_INFO_CHANGED";
    public static final String ACTION_DEVICE_LIST_RECEIVED =
            "io.whisper.demo.DEVICE_LIST_RECEIVED";
    public static final String ACTION_DEVICE_ADDED =
            "io.whisper.demo.DEVICE_ADDED";
    public static final String ACTION_DEVICE_REMOVED =
            "io.whisper.demo.DEVICE_REMOVED";
    public static final String ACTION_DEVICE_STATUS_CHANGED =
            "io.whisper.demo.DEVICE_STATUS_CHANGED";

	private static final String appId  = "HMWL2aNJKnyjtL7K3e7fCHxFVQ9fCpSW8xvpJG3LtFWW";
	private static final String appKey = "8e9VnqPJw5NbK2QztwpyzwysT5yQ84i3vWB43wxy2BJz";
	private static final String apiServerUrl  = "https://whisper.freeddns.org:8443/web/api";
	private static final String mqttServerUri = "ssl://whisper.freeddns.org:8883";
	private static final String stunServer  = "whisper.freeddns.org";
	private static final String turnServer = "whisper.freeddns.org";
	private static final String turnUsername = "whisper";
	private static final String turnPassword = "io2016whisper";

    private static final String TAG = DeviceManager.class.getSimpleName();
    private static final int MSG_SET_BRIGHTNESS = 1;

	private static DeviceManager deviceManager = null;

	private Whisper whisperInstance = null;
    private ConnectionStatus status = ConnectionStatus.Disconnected;
	private List<Device> devices = new ArrayList<Device>();
    private Map<String, Device> deviceMap = new HashMap();

    private boolean isBulbOn = false;
    private boolean isTorchOn = false;
    private CameraManager cameraManager = null;
    private String cameraId = null;
    private boolean isTorchAvailbale = false;
    private MediaPlayer audioPlayer = null;
    private float audioVolume = 1;

    public DeviceManager() {
		super();
	}

	public static DeviceManager sharedManager() {
		if (deviceManager == null) {
			deviceManager = new DeviceManager();
		}
		return deviceManager;
	}

    public ConnectionStatus getStatus()
    {
        return status;
    }

	public List<Device> getDevices()
	{
        return devices;
	}

    public Device getDevice(String deviceId)
    {
        return deviceMap.get(deviceId);
    }

	public void start() {
        if (whisperInstance == null) {
            File certFile = new File(MainApp.getAppContext().getFilesDir().getAbsolutePath(), "whisper.pem");
            if (!certFile.exists()) {
                try {
                    InputStream is = MainApp.getAppContext().getAssets().open("whisper.pem");
                    FileOutputStream fos = new FileOutputStream(certFile);
                    byte[] buffer = new byte[1024 * 1024];
                    int count;
                    while ((count = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, count);
                    }
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error copying whisper.pem to " + certFile, e);
                    e.printStackTrace();
                    return;
                }
            }

            String deviceId = ((TelephonyManager) MainApp.getAppContext().
                    getSystemService(MainApp.getAppContext().TELEPHONY_SERVICE)).getDeviceId();

            Whisper.Options options = new Whisper.Options();
            options.setAppId(appId);
            options.setAppKey(appKey);
            options.setApiServerUrl(apiServerUrl);
            options.setMqttServerUri(mqttServerUri);
            options.setTrustStore(certFile.getAbsolutePath());
            options.setPersistentLocation(MainApp.getAppContext().getFilesDir().getAbsolutePath());
            options.setDeviceId(deviceId);
            options.setConnectTimeout(5);
            options.setRetryInterval(1);

            try {
                Log.i(TAG, "Ready to get whisper instance");
                whisperInstance = Whisper.getInstance(options, DeviceManager.this);
                Log.i(TAG, "Whisper instance created");

                whisperInstance.start(1000);
                Log.i(TAG, "Whisper is running now");
            } catch (WhisperException e) {
                Log.e(TAG, "Get Whisper instance error : " + e.getErrorCode());
            }
        }
	}
	
	public UserInfo getSelfInfo() {
        if (whisperInstance == null) {
            return null;
        }

		try {
			return whisperInstance.getSelfInfo();
		} catch (WhisperException e) {
			e.printStackTrace();
		}

        return null;
	}

	public void pair(String deviceId) throws WhisperException {
		whisperInstance.friendRequest(deviceId, "password");
	}

	public void unPair(Device device) throws WhisperException {
		whisperInstance.removeFriend(device.getDeviceId());
	}
	
	public void setDeviceName(String deviceId, String deviceName) {
		try {
			whisperInstance.LabelFriend(deviceId, deviceName);
		} catch (WhisperException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		for (Device device : devices) {
			device.disconnect();
		}

		devices.clear();
        deviceMap.clear();

		Manager sessionManager = Manager.getInstance();
		if (sessionManager != null) {
			sessionManager.cleanup();
		}

		whisperInstance.kill();
		whisperInstance = null;
	}

	@Override
	public void onIdle(Whisper whisper) {
	}

	@Override
	public void onConnection(Whisper whisper, ConnectionStatus status) {
		Log.i(TAG, "OnConnection:" + status);

        this.status = status;
		if (status == ConnectionStatus.Disconnected) {
			for (Device device : devices) {
				device.disconnect();
			}
            devices.clear();
            deviceMap.clear();
		}

        Intent intent = new Intent(ACTION_CONNECTION_STATUS_CHANGED);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onReady(Whisper whisper) {
		Log.i(TAG, "onReady emitted");

        try {
            UserInfo userInfo = whisperInstance.getSelfInfo();
            if (userInfo.getName().length() == 0) {
                String manufacturer = android.os.Build.MANUFACTURER;
                String clientName = android.os.Build.MODEL;
                if (!clientName.startsWith(manufacturer)) {
                    clientName = manufacturer + " " + clientName;
                }

                if (clientName.length() > 0) {
                    if (clientName.length() > 32) {
                        clientName = clientName.substring(0, 31);
                    }

                    userInfo.setName(clientName);
                    whisperInstance.setSelfInfo(userInfo);
                }
            }

            Manager.Options options = new Manager.Options();
            options.setTransports(Manager.Options.TRANSPORT_ICE);
            options.setStunHost(stunServer);
            options.setTurnHost(turnServer);
            options.setTurnUserName(turnUsername);
            options.setTurnPassword(turnPassword);
            Manager.getInstance(whisper, options);
        }
        catch (WhisperException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void onSelfInfoChanged(Whisper whisper, UserInfo userInfo) {
		Log.i(TAG, "onSelfInfoChanged:" + userInfo);
        Intent intent = new Intent(ACTION_SELF_INFO_CHANGED);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriends(Whisper whisper, List<FriendInfo> friends) {
		Log.i(TAG, "onFriends:" + friends);

		for (FriendInfo friendInfo : friends) {
			Device device = new Device();
            device.deviceInfo = friendInfo;
            device.online = friendInfo.getPresence().equals("online");
            devices.add(device);
            deviceMap.put(friendInfo.getUserId(), device);
		}

        Intent intent = new Intent(ACTION_DEVICE_LIST_RECEIVED);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriendInfoChanged(Whisper whisper, String friendId, FriendInfo friendInfo) {
		Log.i(TAG, "onFriendInfoChanged:" + friendInfo);

        deviceMap.get(friendId).deviceInfo = friendInfo;

        Intent intent = new Intent(ACTION_DEVICE_INFO_CHANGED);
        intent.putExtra("deviceId", friendId);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriendPresence(Whisper whisper, String friendId, String presence) {
		Log.i(TAG, "onFriendPresenceChanged: friendId:" + friendId + ",Presence:" + presence);

        deviceMap.get(friendId).online = presence.equals("online");

        Intent intent = new Intent(ACTION_DEVICE_INFO_CHANGED);
        intent.putExtra("deviceId", friendId);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriendAdded(Whisper whisper, FriendInfo friendInfo) {
		Log.i(TAG, "onFriendAdded:" + friendInfo);

		Device device = new Device();
        device.deviceInfo = friendInfo;
        device.online = friendInfo.getPresence().equals("online");
        devices.add(device);
        deviceMap.put(friendInfo.getUserId(), device);

        Intent intent = new Intent(ACTION_DEVICE_ADDED);
        intent.putExtra("deviceId", friendInfo.getUserId());
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriendRemoved(Whisper whisper, String friendId) {
		Log.i(TAG, "onFriendRemoved:" + friendId);

        Device device = deviceMap.remove(friendId);
        device.disconnect();
        if (!devices.remove(device)) {
            Log.e(TAG, "remove device error");
        }

        Intent intent = new Intent(ACTION_DEVICE_REMOVED);
        intent.putExtra("deviceId", friendId);
        MainApp.getAppContext().sendBroadcast(intent);
	}

	@Override
	public void onFriendRequest(Whisper whisper, String userId, UserInfo userInfo,
								   String hello) {
		Log.i(TAG, "onFriendRequest:" + "from:" + userId +
				", with info:" + userInfo + "and mesaage:" + hello);

        try {
            whisper.replyFriendRequest(userId, 0, null, true, null);
        }
        catch (WhisperException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void onFriendResponse(Whisper whisper, String userId, int status, String reason,
								 boolean entrusted, String expire) {
		Log.i(TAG, "onFriendResponse: from:" + userId);
		if (status == 0) {
			Log.i(TAG, "Friend request acknowleged with entrusted mode " + entrusted + ")");
		} else {
			Log.i(TAG, "Friend request rejected with reason (" + reason + ")");
		}
	}

	@Override
	public void onFriendMessage(Whisper whisper, String from, String message) {
		Log.i(TAG, "onFriendMessage from: " + from + ", with message: " + message);

        try {
            JSONObject msg = new JSONObject(message);
            String msgType = msg.getString("type");
            if (msgType.equals("query")) {
                JSONObject selfStatus = new JSONObject(getDeviceStatus(null));
                sendMessage(selfStatus, from);
            }
            else if (msgType.equals("modify")) {
                Object bulb = msg.opt("bulb");
                if (bulb != null) {
                    setBulbStatus((Boolean)bulb, null);
                }

                Object torch = msg.opt("torch");
                if (torch != null) {
                    setTorchStatus((Boolean)torch, null);
                }

                Object brightness = msg.opt("brightness");
                if (brightness != null) {
                    Message handlerMessage = Message.obtain();
                    handlerMessage.what = MSG_SET_BRIGHTNESS;
                    handlerMessage.obj = brightness;
                    mainHandler.sendMessage(handlerMessage);
                }

                Object ring = msg.opt("ring");
                if (ring != null) {
                    if ((Boolean)ring) {
                        startRing(null);
                    }
                    else {
                        stopRing(null);
                    }
                }

                Object volume = msg.opt("volume");
                if (volume != null) {
                    setVolume(((Number)volume).floatValue(), null);
                }
            }
            else if (msgType.equals("status") || msgType.equals("sync")) {
                HashMap<String, Object> newStatus = new HashMap();
                Iterator<String> keys = msg.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    newStatus.put(key, msg.get(key));
                }

                Intent intent = new Intent(ACTION_DEVICE_STATUS_CHANGED);
                intent.putExtra("deviceId", from.split("@")[0]);
                intent.putExtra("status", newStatus);
                MainApp.getAppContext().sendBroadcast(intent);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void onFriendInviteRequest(Whisper whisper, String from, String message) {
		Log.i(TAG, "onFriendInviteRequest from: " + from + ", with hell message: " + message);
	}

    private Handler mainHandler = new MainHandler();
    private class MainHandler extends Handler {

        public MainHandler(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SET_BRIGHTNESS:
                    setBrightness(((Number)msg.obj).floatValue(), null);
                    break;
                default:
                    break;
            }
        }
    };

    private CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(String cameraId) {
            super.onTorchModeUnavailable(cameraId);

            if (cameraId.equals(DeviceManager.this.cameraId)) {
                isTorchAvailbale = false;

                if (isTorchOn) {
                    isTorchOn = false;

                    HashMap<String, Object> msg = new HashMap();
                    msg.put("torch", false);
                    syncSelfStatus(msg);
                }
            }
        }

        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);

            if (cameraId.equals(DeviceManager.this.cameraId)) {
                isTorchAvailbale = true;

                if (isTorchOn != enabled) {
                    isTorchOn = enabled;

                    HashMap<String, Object> msg = new HashMap();
                    msg.put("torch", enabled);
                    syncSelfStatus(msg);
                }
            }
        }
    };

    private void initCamera() {
        try {
            if (cameraManager == null) {
                cameraManager = (CameraManager) MainApp.getAppContext().getSystemService(Context.CAMERA_SERVICE);
                cameraManager.registerTorchCallback(torchCallback, new Handler(Looper.getMainLooper()));
            }

            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // 过滤掉前置摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                this.cameraId = cameraId;

                // 判断设备是否支持闪光灯
                this.isTorchAvailbale = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (this.isTorchAvailbale) {
                    break;
                }
            }
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private boolean sendMessage(JSONObject msg, String to) {
        boolean result = false;

        try {
            whisperInstance.sendFriendMessage(to, msg.toString());
            result = true;
        } catch (WhisperException e) {
            e.printStackTrace();
        }

        return result;
    }

    private boolean sendMessage(JSONObject msg, Device device) {
        return device.online ? sendMessage(msg, device.getDeviceId()) : false;
    }

    private void syncSelfStatus(HashMap<String, Object> selfStatus) {
        selfStatus.put("type", "sync");

        Intent intent = new Intent(ACTION_DEVICE_STATUS_CHANGED);
        intent.putExtra("status", selfStatus);
        MainApp.getAppContext().sendBroadcast(intent);

        if (status == ConnectionStatus.Connected && devices.size() > 0) {
            JSONObject jsonObject = new JSONObject(selfStatus);

            for (Device dev : devices) {
                sendMessage(jsonObject, dev);
            }
        }
    }

    public HashMap<String, Object> getDeviceStatus(Device device) {
        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "query");
                sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        HashMap<String, Object> selfStatus = new HashMap();
        selfStatus.put("type", "status");
        selfStatus.put("bulb", isBulbOn);

        if (cameraId == null) {
            initCamera();
        }
        if (isTorchAvailbale) {
            selfStatus.put("torch", isTorchOn);
        }

        selfStatus.put("brightness", MainApp.getScreenBrightness());

        selfStatus.put("ring", audioPlayer != null ? audioPlayer.isPlaying() : false);
        selfStatus.put("volume", audioVolume);

        return selfStatus;
    }

    public boolean setBulbStatus(boolean on, Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("bulb", on);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            isBulbOn = on;
            result = true;

            HashMap<String, Object> msg = new HashMap();
            msg.put("bulb", on);
            syncSelfStatus(msg);
        }

        return result;
    }

    public boolean setTorchStatus(boolean on, Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("torch", on);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                if (cameraId == null) {
                    initCamera();
                }

                if (isTorchAvailbale) {
                    cameraManager.setTorchMode(cameraId, on);
                    result = true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public boolean setBrightness(float brightness, Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("brightness", brightness);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            MainApp.setScreenBrightness(brightness);
            result = true;

            HashMap<String, Object> msg = new HashMap();
            msg.put("brightness", brightness);
            syncSelfStatus(msg);
        }

        return result;
    }

    public boolean startRing(Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("ring", true);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            if (audioPlayer == null) {
                audioPlayer = MediaPlayer.create(MainApp.getAppContext(), R.raw.audio);
                audioPlayer.setLooping(true);

                audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        HashMap<String, Object> msg = new HashMap();
                        msg.put("ring", false);
                        syncSelfStatus(msg);
                    }
                });
            }

            audioPlayer.start();
            audioPlayer.setVolume(audioVolume, audioVolume);

            result = true;

            HashMap<String, Object> msg = new HashMap();
            msg.put("ring", true);
            syncSelfStatus(msg);
        }

        return result;
    }

    public boolean stopRing(Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("ring", false);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            if (audioPlayer != null && audioPlayer.isPlaying()) {
                audioPlayer.pause();
            }

            result = true;

            HashMap<String, Object> msg = new HashMap();
            msg.put("ring", false);
            syncSelfStatus(msg);
        }

        return result;
    }

    public boolean setVolume(float volume, Device device) {
        boolean result = false;

        if (device != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "modify");
                jsonObject.put("volume", volume);
                result = sendMessage(jsonObject, device);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            audioVolume = volume;
            if (audioPlayer != null) {
                audioPlayer.setVolume(volume, volume);
            }

            result = true;

            HashMap<String, Object> msg = new HashMap();
            msg.put("volume", volume);
            syncSelfStatus(msg);
        }

        return result;
    }
}
