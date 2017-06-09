package io.whisper.demo.device;

import android.content.Intent;
import android.os.AsyncTask;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.whisper.demo.MainApp;

import io.whisper.managed.core.FriendInfo;

public class Device {

	public FriendInfo deviceInfo;
	public boolean online;

	public String getDeviceId() {
		return deviceInfo.getUserId();
	}

	public String getDeviceName() {
		String deviceName = deviceInfo.getLabel();
		if (deviceName == null || deviceName.length() == 0) {
			deviceName = deviceInfo.getName();
			if (deviceName == null || deviceName.length() == 0) {
				deviceName = deviceInfo.getUserId();
			}
		}

		return deviceName;
	}

	private ConnectTask connectTask;
//	private int sessionId;

	public boolean connect()
	{
		return connect(false);
	}

	public boolean connect(boolean allowUpdatePort)
	{
//		if (sessionId > 0 && portFordwordId > 0)

		if (!online)
			return false;

		if (connectTask == null) {
			connectTask = new ConnectTask();
			connectTask.allowUpdatePort = allowUpdatePort;
			connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		return false;
	}

	public void disconnect()
	{
//		if (sessionId > 0) {
//			new DisconnectTask()..executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionId, portFordwordId);
//		}

//		sessionId = 0;
	}
	
//	private Handler mainHandler = new MainHandler();
//	private class MainHandler extends Handler {
//
//		public MainHandler(){
//			super(Looper.getMainLooper());
//		}
//
//        @Override
//        public void handleMessage(Message msg) {
//			super.handleMessage(msg);
//        	switch (msg.what) {
//            case 1:
//            	onSessionReceivedData(msg.arg1, msg.arg2, (byte[]) msg.obj);
//                break;
//            case 2:
//            	onSessionClosed(msg.arg1, msg.arg2);
//                break;
//            default:
//                break;
//            }
//        }
//	};
	
	private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
		boolean allowUpdatePort = false;
		private ReentrantLock lock;
		private Condition condition;

		@Override
		protected Boolean doInBackground(Void... params) {
//			if (sessionId == 0) {vv
//				int result = SessionManager.getInstance().connect(deviceId, "", 3, new PeerSessionHandler() {
//					@Override
//					public void onSessionReceivedData(int sessionId, int channelId, byte[] data)
//					{
//						Message msg = new Message();
//						msg.what = 1;
//						msg.arg1 = sessionId;
//						msg.arg2 = channelId;
//						msg.obj = data;
//						mainHandler.sendMessage(msg);
//					}
//
//					@Override
//					public void onSessionClosed(int sessionId, int status)
//					{
//						Message msg = new Message();
//						msg.what = 2;
//						msg.arg1 = sessionId;
//						msg.arg2 = status;
//						mainHandler.sendMessage(msg);
//					}
//				});
//
//				if (result <= 0) {
//					Log.e("ConnectTask", "connect failed");
//					return null;
//				} else {
//					sessionId = result;
//					portFordwordId = 0;
//					Log.d("ConnectTask", "Session mode: " + SessionManager.getInstance().getSessionInfo(sessionId).getMode());
//				}
//			}

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			connectTask = null;
			if (result) {
				Intent intent = new Intent();
				intent.setAction("DeviceConnected");
				intent.putExtra("deviceId", Device.this.getDeviceId());
				MainApp.getAppContext().sendBroadcast(intent);
			}
			else {
			}
		}
	}
	
	private class DisconnectTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			return null;
		}
	}

//	public void onSessionReceivedData(int sessionId, int channelId, byte[] data)
//	{
//		if (sessionId != this.sessionId) {
//			return;
//		}
//	}
//
//	public void onSessionClosed(int sessionId, int status)
//	{
//		if (sessionId != this.sessionId) {
//			return;
//		}
//
//		this.sessionId = 0;
//	}

}
