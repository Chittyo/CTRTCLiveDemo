package com.example.rtcdemo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rtcdemo.R;

import cn.rongcloud.rtc.base.RCRTCLiveRole;

public class LivePrepareActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = LivePrepareActivity.class.getName();
    private EditText etRoomId;
    private Button btnStartLive, btnJoinLive;
    public static final String USER_ID = "user_id";
    private String userId = "001";
    private String roomId = "1001";

    public static void start(Context context, String userId) {
        Intent intent = new Intent(context, LivePrepareActivity.class);
        intent.putExtra(USER_ID, userId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_prepare);

        etRoomId = findViewById(R.id.etRoomId);
        btnStartLive = findViewById(R.id.btnStartLive);
        btnJoinLive = findViewById(R.id.btnJoinLive);

        btnStartLive.setOnClickListener(this);
        btnJoinLive.setOnClickListener(this);

        Intent intent = getIntent();
        userId = intent.getStringExtra(USER_ID);
    }

    @Override
    public void onClick(View v) {
        String roomId = etRoomId.getText().toString().trim();
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "请输入直播房间 ID", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()){
            case R.id.btnStartLive:
                LiveActivity.start(LivePrepareActivity.this, roomId, userId, RCRTCLiveRole.BROADCASTER.getType());
                break;
            case R.id.btnJoinLive:
                LiveActivity.start(LivePrepareActivity.this, roomId, userId, RCRTCLiveRole.AUDIENCE.getType());
                break;
        }
    }
}
