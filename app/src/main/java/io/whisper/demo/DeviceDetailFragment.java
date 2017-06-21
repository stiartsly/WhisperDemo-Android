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
import android.view.LayoutInflater;
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
public class DeviceDetailFragment extends Fragment {

    private static final int MSG_SET_BULB_FAILED = 1;
    private static final int MSG_SET_TORCH_FAILED = 2;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The device this fragment is presenting.
     */
    private Device mDevice;

    private ImageView mIconBulb;
    private SwitchButton mSwitchBulb;
    private SwitchButton mSwitchTorch;
    private SeekBar mSeekBarBrightness;
    private ImageButton mButtonRing;
    private SeekBar mSeekBarVolume;

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
        mIconBulb = (ImageView) rootView.findViewById(R.id.iconBulb);
        mSwitchBulb = (SwitchButton) rootView.findViewById(R.id.switchBulb);
        mSwitchTorch = (SwitchButton) rootView.findViewById(R.id.switchTorch);
        mSeekBarBrightness = (SeekBar) rootView.findViewById(R.id.seekBarBrightness);
        mButtonRing = (ImageButton) rootView.findViewById(R.id.buttonAudioPlay);
        mSeekBarVolume = (SeekBar) rootView.findViewById(R.id.seekBarVolume);

        mSwitchBulb.setOnCheckedChangeListener(switchBulbCheckedChange);
        mSwitchTorch.setOnCheckedChangeListener(switchTorchCheckedChange);
        mSeekBarBrightness.setProgress(mSeekBarBrightness.getMax() / 2);
        mSeekBarBrightness.setOnSeekBarChangeListener(seekBarBrightnessChangeListener);
        mButtonRing.setOnClickListener(buttonRingOnClick);
        mSeekBarVolume.setProgress(mSeekBarVolume.getMax());
        mSeekBarVolume.setOnSeekBarChangeListener(seekBarVolumeChangeListener);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        HashMap<String, Object> status = DeviceManager.sharedManager().getDeviceStatus(mDevice);
        if (status != null) {
            updateView(status);
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onDestroy();
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
        else if (((String) status.get("type")).equals("status")) {
            mSwitchTorch.setChecked(false);
            mSwitchTorch.setEnabled(false);
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
                case MSG_SET_TORCH_FAILED: {
                    boolean oldStatus = msg.arg1 == 1;
                    mSwitchTorch.setChecked(oldStatus);
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

                        Message msg = Message.obtain();
                        msg.what = MSG_SET_TORCH_FAILED;
                        msg.arg1 = isChecked ? 0 : 1;
                        mHandler.sendMessage(msg);
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
}
