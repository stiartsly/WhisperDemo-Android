package io.whisper.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

import java.util.HashMap;

import io.whisper.demo.device.Device;
import io.whisper.demo.device.DeviceManager;

/**
 * A fragment representing a single Device detail screen.
 * This fragment is either contained in a {@link DeviceListActivity}
 * in two-pane mode (on tablets) or a {@link DeviceDetailActivity}
 * on handsets.
 */
public class DeviceDetailFragment extends Fragment implements SurfaceHolder.Callback {

    private static final int MSG_SET_BULB_FAILED = 1;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The device this fragment is presenting.
     */
    private Device mDevice;

    private View mViewBulb;
    private ImageView mIconBulb;
    private SwitchButton mSwitchBulb;
    private View mViewTorch;
    private SwitchButton mSwitchTorch;
    private View mViewBrightness;
    private SeekBar mSeekBarBrightness;
    private View mViewRing;
    private ImageButton mButtonRing;
    private SeekBar mSeekBarVolume;
    private View mViewVideo;
    private View mVideoView;
    private ImageButton mButtonVideo;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DeviceDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String deviceId = getArguments().getString(ARG_ITEM_ID);
            mDevice = DeviceManager.sharedManager().getDevice(deviceId);
        }

        Activity activity = this.getActivity();
        if (mDevice != null) {
            activity.setTitle(mDevice.getDeviceName());
        }
        else {
            activity.setTitle("本机");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceManager.ACTION_DEVICE_STATUS_CHANGED);
        activity.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.device_detail, container, false);

        mViewBulb = rootView.findViewById(R.id.device_detail_example);
        mIconBulb = (ImageView) mViewBulb.findViewById(R.id.iconBulb);
        mSwitchBulb = (SwitchButton) mViewBulb.findViewById(R.id.switchBulb);
        mSwitchBulb.setOnCheckedChangeListener(switchBulbCheckedChange);

        mViewTorch = rootView.findViewById(R.id.device_detail_torch);
        mSwitchTorch = (SwitchButton) mViewTorch.findViewById(R.id.switchTorch);
        mSwitchTorch.setOnCheckedChangeListener(switchTorchCheckedChange);

        mViewBrightness = rootView.findViewById(R.id.device_detail_brightness);
        mSeekBarBrightness = (SeekBar) mViewBrightness.findViewById(R.id.seekBarBrightness);
        mSeekBarBrightness.setOnSeekBarChangeListener(seekBarBrightnessChangeListener);

        mViewRing = rootView.findViewById(R.id.device_detail_audio);
        mButtonRing = (ImageButton) mViewRing.findViewById(R.id.buttonAudioPlay);
        mButtonRing.setOnClickListener(buttonRingOnClick);
        mSeekBarVolume = (SeekBar) mViewRing.findViewById(R.id.seekBarVolume);
        mSeekBarVolume.setOnSeekBarChangeListener(seekBarVolumeChangeListener);

        mViewVideo = rootView.findViewById(R.id.device_detail_video);
        mVideoView = mViewVideo.findViewById(R.id.videoView);
        if (!VideoDecoder.USE_FFMPEG) {
            ((SurfaceView)mVideoView).getHolder().addCallback(this);
        }
        mButtonVideo = (ImageButton) mViewVideo.findViewById(R.id.buttonVideoPlay);
        mButtonVideo.setOnClickListener(buttonVideoOnClick);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mButtonVideo.isSelected()) {
            if (mDevice != null) {
                mDevice.stopVideo();
            }
            else {
                DeviceManager.sharedManager().stopVideo();
            }
            mButtonVideo.setSelected(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDevice == null) {
            DeviceManager.sharedManager().getDeviceStatus(mDevice,
                    new DeviceManager.TaskCompletionListener() {
                        @Override
                        public void onSuccess(HashMap<String, Object> result) {
                            configureView(result);
                        }

                        @Override
                        public void onError(Exception exception) {

                        }
                    });
        }
        else {
            configureView(mDevice.status);
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("Surface", "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.i("Surface", "surfaceChanged, width = " + width + ", height = " + height);
        Surface surface = holder.getSurface();
        Log.d("Surface", "surface = " + surface);
        if (mButtonVideo.isSelected()) {
            if (mDevice != null) {
                mDevice.startVideo(mVideoView);
            }
            else {
                DeviceManager.sharedManager().startVideo(mVideoView);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("Surface", "surfaceDestroyed");
        if (mButtonVideo.isSelected()) {
            if (mDevice != null) {
                mDevice.stopVideo();
            }
            else {
                DeviceManager.sharedManager().stopVideo();
            }
            mButtonVideo.setSelected(false);
        }
    }

    private void configureView(HashMap<String, Object> status) {
        Object bulb = status.get("bulb");
        if (bulb != null) {
            mIconBulb.setSelected((Boolean) bulb);
            mSwitchBulb.setChecked((Boolean) bulb);
        }
        else {
            mViewBulb.setVisibility(View.GONE);
        }

        Object torch = status.get("torch");
        if (torch != null) {
            mSwitchTorch.setChecked((Boolean) torch);
            mSwitchTorch.setEnabled(true);
        }
        else {
            mViewTorch.setVisibility(View.GONE);
        }

        Object brightness = status.get("brightness");
        if (brightness != null) {
            int newValue = (int) (((Number) brightness).floatValue() * mSeekBarBrightness.getMax());
            mSeekBarBrightness.setProgress(newValue);
        }
        else {
            mViewBrightness.setVisibility(View.GONE);
        }

        Object ring = status.get("ring");
        if (ring != null) {
            mButtonRing.setSelected((Boolean)ring);
            Object volume = status.get("volume");
            if (volume != null) {
                int newValue = (int) (((Number) volume).floatValue() * mSeekBarVolume.getMax());
                mSeekBarVolume.setProgress(newValue);
            }
        }
        else {
            mViewRing.setVisibility(View.GONE);
        }

        Object video = status.get("camera");
        if (video != null) {
            mButtonVideo.setSelected(false); // (Boolean)video
        }
        else {
            mViewVideo.setVisibility(View.GONE);
        }
    }

    private void updateView(HashMap<String, Object> status) {
        Object bulb = status.get("bulb");
        if (bulb != null) {
            mIconBulb.setSelected((Boolean) bulb);
            mSwitchBulb.setChecked((Boolean) bulb);
        }

        Object torch = status.get("torch");
        if (torch != null) {
            mSwitchTorch.setChecked((Boolean) torch);
            mSwitchTorch.setEnabled(true);
        }

        Object brightness = status.get("brightness");
        if (brightness != null) {
            int newValue = (int) (((Number) brightness).floatValue() * mSeekBarBrightness.getMax());
            mSeekBarBrightness.setProgress(newValue);
        }

        Object ring = status.get("ring");
        if (ring != null) {
            mButtonRing.setSelected((Boolean)ring);
        }

        Object volume = status.get("volume");
        if (volume != null) {
            int newValue = (int) (((Number) volume).floatValue() * mSeekBarVolume.getMax());
            mSeekBarVolume.setProgress(newValue);
        }

        Object video = status.get("camera");
        if (video != null) {
            mButtonVideo.setSelected((Boolean)video);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String deviceId = bundle.getString("deviceId", null);

            if ((deviceId != null && mDevice != null && deviceId.equals(mDevice.getDeviceId())) ||
                    (deviceId == null && mDevice == null)) {
                updateView((HashMap) bundle.get("status"));
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SET_BULB_FAILED: {
                    boolean oldStatus = msg.arg1 == 1;
                    mSwitchBulb.setChecked(oldStatus);
                }
                    break;
            }
        }
    };

    private SwitchButton.OnCheckedChangeListener switchBulbCheckedChange =
            new SwitchButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                    if (!DeviceManager.sharedManager().setBulbStatus(isChecked, mDevice)) {
                        Toast.makeText(getActivity(), "操作失败", Toast.LENGTH_SHORT).show();

                        Message msg = Message.obtain();
                        msg.what = MSG_SET_BULB_FAILED;
                        msg.arg1 = isChecked ? 0 : 1;
                        mHandler.sendMessage(msg);
                    }
                }
            };

    private SwitchButton.OnCheckedChangeListener switchTorchCheckedChange =
            new SwitchButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                    if (!DeviceManager.sharedManager().setTorchStatus(isChecked, mDevice)) {
                        Toast.makeText(getActivity(), "操作失败", Toast.LENGTH_SHORT).show();

                        final boolean oldStatus = !isChecked;
                        mSwitchTorch.post(new Runnable() {
                            @Override
                            public void run() {
                                mSwitchTorch.setChecked(oldStatus);
                            }
                        });
                    }
                }
            };

    private SeekBar.OnSeekBarChangeListener seekBarBrightnessChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBar.setTag(seekBar.getProgress());
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    float newValue = ((float) seekBar.getProgress()) / seekBar.getMax();
                    if (!DeviceManager.sharedManager().setBrightness(newValue, mDevice)) {
                        Toast.makeText(getActivity(), "修改亮度失败", Toast.LENGTH_SHORT).show();

                        Integer oldValue = (Integer) seekBar.getTag();
                        seekBar.setProgress(oldValue);
                    }
                }
            };

    private View.OnClickListener buttonRingOnClick =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean playing = mButtonRing.isSelected();
                    if (playing) {
                        if (DeviceManager.sharedManager().stopRing(mDevice)) {
                            mButtonRing.setSelected(false);
                        }
                        else {
                            Toast.makeText(getActivity(), "停止铃声失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        if (DeviceManager.sharedManager().startRing(mDevice)) {
                            mButtonRing.setSelected(true);
                        }
                        else {
                            Toast.makeText(getActivity(), "开启铃声失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

    private SeekBar.OnSeekBarChangeListener seekBarVolumeChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBar.setTag(seekBar.getProgress());
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    float newValue = ((float) seekBar.getProgress()) / seekBar.getMax();
                    if (!DeviceManager.sharedManager().setVolume(newValue, mDevice)) {
                        Toast.makeText(getActivity(), "修改音量失败", Toast.LENGTH_SHORT).show();

                        Integer oldValue = (Integer) seekBar.getTag();
                        seekBar.setProgress(oldValue);
                    }
                }
            };

    private View.OnClickListener buttonVideoOnClick =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean playing = mButtonVideo.isSelected();
                    if (playing) {
                        if (mDevice != null) {
                            mDevice.stopVideo();
                        }
                        else {
                            DeviceManager.sharedManager().stopVideo();
                        }
                        mButtonVideo.setSelected(false);
                    }
                    else {
                        if (mDevice != null) {
                            if (mDevice.startVideo(mVideoView)) {
                                mButtonVideo.setSelected(true);
                            }
                            else {
                                Toast.makeText(getActivity(), "开启远程摄像头失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            if (DeviceManager.sharedManager().startVideo(mVideoView)) {
                                mButtonVideo.setSelected(true);
                            }
                            else {
                                Toast.makeText(getActivity(), "开启摄像头失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            };
}
