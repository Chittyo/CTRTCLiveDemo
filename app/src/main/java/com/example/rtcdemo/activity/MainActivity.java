package com.example.rtcdemo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.rtcdemo.App;
import com.example.rtcdemo.R;
import com.example.rtcdemo.common.MockAppServer;
import com.example.rtcdemo.common.UiUtils;

import java.util.ArrayList;

import io.rong.imlib.RongIMClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = MainActivity.class.getName();
    private String token001 = "Enzbxdr7hdO6WSr+DZFARkaUNlc6QSw8FXTjaWyaqiE=@poxt.cn.rongnavonUserLeft.com;poxt.cn.rongcfg.com";
    private String token002 = "DmWWu3/S6666WSr+DZFARi8Xh3c+CQbNFXTjaWyaqiE=@poxt.cn.rongnav.com;poxt.cn.rongcfg.com";
    private String userId = "001", token = token001;
    private Button btnGetTokenConnectIMServer, btnRTCConnectIMServer;
    private RadioGroup rgUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetTokenConnectIMServer = findViewById(R.id.btnGetTokenConnectIMServer);
        btnRTCConnectIMServer = findViewById(R.id.btnRTCConnectIMServer);
        rgUsers = findViewById(R.id.rgUsers);

        btnGetTokenConnectIMServer.setOnClickListener(this);
        btnRTCConnectIMServer.setOnClickListener(this);
        rgUsers.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rbUser001:
                        userId = "001";
                        token = token001;//???????????????token
                        break;
                    case R.id.rbUser002:
                        userId = "002";
                        token = token002;//???????????????token
                        break;

                }
            }
        });
    }

    // ?????????????????????????????????
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissionList = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            };
            ArrayList<String> ungrantedPermissions = new ArrayList<>();
            for (String permission : permissionList) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ungrantedPermissions.add(permission);
                }
            }
            if (!ungrantedPermissions.isEmpty()) {
                String[] array = new String[ungrantedPermissions.size()];
                ActivityCompat.requestPermissions(this, ungrantedPermissions.toArray(array), 0);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int grantedCount = 0;
        for (int ret : grantResults) {
            if (PackageManager.PERMISSION_GRANTED == ret) {
                grantedCount++;
            }
        }
        if (grantedCount == permissions.length) {
            getTokenFromAppServer();
        } else {
            Toast.makeText(this, "???????????????????????????", Toast.LENGTH_SHORT).show();
        }
    }

    // ?????????????????? UserID????????????????????? App Server ?????? Token???
    private void getTokenFromAppServer() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "UserID ???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        UiUtils.showWaitingDialog(this);
        MockAppServer.getToken(App.APP_KEY, App.APP_SECRET, userId, new MockAppServer.GetTokenCallback() {

            @Override
            public void onGetTokenSuccess(String token) {
                Log.d(TAG, "--> onGetTokenSuccess() token = " + token);
                rtcConnectIMServer(token);
            }

            @Override
            public void onGetTokenFailed(String code) {
                UiUtils.hideWaitingDialog();
                Log.e(TAG,"--> onGetTokenFailed() ?????? Token ?????????code = " + code);
            }
        });
    }

    private void rtcConnectIMServer(String token) {
        RongIMClient.connect(token, new RongIMClient.ConnectCallback() {
            @Override
            public void onSuccess(String s) {
                UiUtils.hideWaitingDialog();
                LivePrepareActivity.start(MainActivity.this, userId);
            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode code) {
                Log.e(TAG, "--> rtcConnectIMServer - onError - ???????????? IM ???????????????code = " + code);
                UiUtils.hideWaitingDialog();
                if(code.equals(RongIMClient.ConnectionErrorCode.RC_CONN_TOKEN_INCORRECT)) {
                    //??? APP ??????????????? token????????????
                    getTokenFromAppServer();
                }else if (code.equals(RongIMClient.ConnectionErrorCode.RC_CONNECTION_EXIST)){
                    //??????????????????????????????
                    LivePrepareActivity.start(MainActivity.this, userId);
                } else {
                    //???????????? IM ?????????????????????????????????????????????????????????
                }
            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                //????????????????????????????????????????????????
                Log.d(TAG, "--> rtcConnectIMServer - onDatabaseOpened databaseOpenStatus = "+databaseOpenStatus);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnGetTokenConnectIMServer://??????????????????RTCLib????????? token
                if (checkPermission()) {
                    getTokenFromAppServer();
                }
                break;
            case R.id.btnRTCConnectIMServer://??????????????????RTCLib????????? token
                if (checkPermission()) {
                    rtcConnectIMServer(token);
                }
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}