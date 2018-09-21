package io.whisper.demo;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil;
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder;
import io.whisper.demo.device.DeviceManager;
import io.whisper.vanilla.UserInfo;

public class MyInfoActivity extends AppCompatActivity {

    private ImageView mQRCode;
    private TextView mNameView;
    private TextView mDescriptionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true); // 决定左上角图标的右侧是否有向左的小箭头, true有小箭头，并且图标可以点击
        actionBar.setDisplayShowHomeEnabled(false);// 使左上角图标是否显示，如果设成false，则没有程序图标，仅仅就个标题，否则，显示应用程序图标，对应id为android.R.id.home，对应ActionBar.DISPLAY_SHOW_HOME

        mQRCode = (ImageView) findViewById(R.id.QRCode);
        mNameView = (TextView) findViewById(R.id.name);
        mDescriptionView = (TextView) findViewById(R.id.description);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();

        UserInfo userInfo = DeviceManager.sharedManager().getSelfInfo();
        if (userInfo == null) {
            mDescriptionView.setText("尚未成功连接服务器");
        }
        else {
            mNameView.setText(userInfo.getName());
            new CreateQRCodeTask().execute(DeviceManager.sharedManager().getAddress());
        }
    }

    private class CreateQRCodeTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            return QRCodeEncoder.syncEncodeQRCode(params[0],
                    BGAQRCodeUtil.dp2px(MyInfoActivity.this, 150));
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mQRCode.setImageBitmap(bitmap);
        }
    }
}
