package io.whisper.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.whisper.demo.device.Device;
import io.whisper.demo.device.DeviceManager;
import io.whisper.managed.exceptions.WhisperException;

import java.util.List;

/**
 * An activity representing a list of Devices. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link DeviceDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class DeviceListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private DeviceRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DeviceListActivity.this, AddDeviceActivity.class));
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.device_list);
        setupRecyclerView(recyclerView);

        if (findViewById(R.id.device_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        DeviceManager.sharedManager().start();

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceManager.ACTION_CONNECTION_STATUS_CHANGED);
        filter.addAction(DeviceManager.ACTION_DEVICE_INFO_CHANGED);
        filter.addAction(DeviceManager.ACTION_DEVICE_LIST_RECEIVED);
        filter.addAction(DeviceManager.ACTION_DEVICE_ADDED);
        filter.addAction(DeviceManager.ACTION_DEVICE_REMOVED);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_my_info) {
            startActivity(new Intent(this, MyInfoActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void removeDevice(Device device) {
        final Device dev = device;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确定要删除此设备？");
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    DeviceManager.sharedManager().unPair(dev);
                } catch (WhisperException e) {
                    e.printStackTrace();
                    Toast.makeText(DeviceListActivity.this, "删除设备失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("BroadcastReceiver", "action : " + action);
//            String deviceId = intent.getExtras().getString("deviceId");
//            Log.i("BroadcastReceiver", "deviceId : " + deviceId);
            adapter.notifyDataSetChanged();
        }
    };

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        adapter = new DeviceRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);
    }

    public class DeviceRecyclerViewAdapter
            extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {

        private final List<Device> mDevices;

        public DeviceRecyclerViewAdapter() {
            mDevices = DeviceManager.sharedManager().getDevices();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (position == 0) {
                holder.mIcon.setImageResource(R.drawable.local);
                holder.mTitleView.setText("本机");

                switch (DeviceManager.sharedManager().getStatus()) {
                    case Disconnected:
                        holder.mSubtitleView.setText("离线");
                        holder.mSubtitleView.setTextColor(android.graphics.Color.GRAY);
                        holder.mSubtitleView.setVisibility(View.VISIBLE);
                        holder.mStatusIcon.setVisibility(View.INVISIBLE);
                        break;

                    case Connecting:
                        holder.mSubtitleView.setText("连接中");
                        holder.mSubtitleView.setTextColor(android.graphics.Color.BLUE);
                        holder.mSubtitleView.setVisibility(View.VISIBLE);
                        holder.mStatusIcon.setVisibility(View.INVISIBLE);
                        break;

                    default:
                        holder.mSubtitleView.setVisibility(View.GONE);
                        holder.mStatusIcon.setVisibility(View.VISIBLE);
                        break;
                }
            }
            else {
                Device device = mDevices.get(position - 1);
                holder.mDevice = device;

                holder.mIcon.setImageResource(R.drawable.remote);
                holder.mTitleView.setText(device.getDeviceName());
                holder.mSubtitleView.setVisibility(View.GONE);
                holder.mStatusIcon.setVisibility(device.online ? View.VISIBLE : View.INVISIBLE);
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        if (holder.mDevice != null) {
                            arguments.putString(DeviceDetailFragment.ARG_ITEM_ID,
                                    holder.mDevice.getDeviceId());
                        }
                        DeviceDetailFragment fragment = new DeviceDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.device_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, DeviceDetailActivity.class);
                        if (holder.mDevice != null) {
                            intent.putExtra(DeviceDetailFragment.ARG_ITEM_ID,
                                    holder.mDevice.getDeviceId());
                        }

                        context.startActivity(intent);
                    }
                }
            });

            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (holder.mDevice != null) {
                        removeDevice(holder.mDevice);
                    }
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDevices.size() + 1;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mIcon;
            public final TextView mTitleView;
            public final TextView mSubtitleView;
            public final ImageView mStatusIcon;
            public Device mDevice;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIcon = (ImageView) view.findViewById(R.id.icon);
                mTitleView = (TextView) view.findViewById(R.id.title);
                mSubtitleView = (TextView) view.findViewById(R.id.subtitle);
                mStatusIcon = (ImageView) view.findViewById(R.id.statusIcon);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mTitleView.getText() + "'";
            }
        }
    }
}
