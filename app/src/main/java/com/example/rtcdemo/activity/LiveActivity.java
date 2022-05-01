package com.example.rtcdemo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rtcdemo.R;
import com.example.rtcdemo.bean.RoomInfoMessage;
import com.example.rtcdemo.common.AnchorConfig;
import com.example.rtcdemo.stutas.IStatus;
import com.example.rtcdemo.stutas.IdleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.rongcloud.beauty.RCRTCBeautyEngine;
import cn.rongcloud.beauty.RCRTCBeautyFilter;
import cn.rongcloud.beauty.RCRTCBeautyOption;
import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCRemoteUser;
import cn.rongcloud.rtc.api.RCRTCRoom;
import cn.rongcloud.rtc.api.RCRTCRoomConfig;
import cn.rongcloud.rtc.api.callback.IRCRTCResultCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCResultDataCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCRoomEventsListener;
import cn.rongcloud.rtc.api.callback.IRCRTCStatusReportListener;
import cn.rongcloud.rtc.api.callback.IRCRTCVideoOutputFrameListener;
import cn.rongcloud.rtc.api.report.StatusBean;
import cn.rongcloud.rtc.api.report.StatusReport;
import cn.rongcloud.rtc.api.stream.RCRTCInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCLiveInfo;
import cn.rongcloud.rtc.api.stream.RCRTCOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCVideoView;
import cn.rongcloud.rtc.base.RCRTCLiveRole;
import cn.rongcloud.rtc.base.RCRTCMediaType;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.rtc.base.RCRTCRoomType;
import cn.rongcloud.rtc.base.RCRTCStreamType;
import cn.rongcloud.rtc.base.RCRTCVideoFrame;
import cn.rongcloud.rtc.base.RTCErrorCode;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

import static android.widget.Toast.LENGTH_SHORT;
import static cn.rongcloud.rtc.base.RCRTCLiveRole.BROADCASTER;
import static com.example.rtcdemo.common.AnchorConfig.enableSpeaker;

public class LiveActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = LiveActivity.class.getName();
    private Context context;
    public static final String USER_ID = "user_id", ROOM_ID = "room_id", LIVE_ROLE_TYPE = "live_role_type";
    private String roomId = "1001", userId = "001";
    private int liveRoleType = BROADCASTER.getType();// 1:主播, 2:观众
    private Button btnRequestLive, btnCloseCamera, btnCloseMic, btnSwitchCrame, btnEndLive, btnSetRoomProperties, btnLeaveRoom;
    private FrameLayout flLocalUser, flRemoteUser, flFullscreen;
    private RadioGroup rgVideoStream;
    private RadioButton rbTinyStream, rbBigStream;
    private TextView tvStatusReportView;
    private RCRTCRoom mRtcRoom = null;
    private boolean enableTinyStream = false;// true:切为小流，false:切为大流；
    private boolean isAudienceRequestLive = false;//观众上麦
    private String roomInfoMsgKey = "weatherForecast";
    private String roomInfoMsgValue = "天气晴朗";
    private String anchorUserId;

    IStatus idleStatus = new IdleStatus();
    IStatus anchorStatus = new AnchorStatus();
    IStatus audienceStatus = new AudienceStatus();

    IStatus curStatus;// 当前用户的状态，可以设置为主播和观众
    IStatus bakStatus;// 用于切换状态失败时恢复状态的备份变量

    List<RCRTCInputStream> mInputStreamList = new ArrayList<>();


    private IRCRTCStatusReportListener statusReportListener = new IRCRTCStatusReportListener() {
        @Override
        public void onConnectionStats(final StatusReport statusReport) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    StatusBean statusBean = new StatusBean();
//                    int audioLevel = statusBean.audioLevel;

                    StatusBean audioBean = null;
                    for (StatusBean bean : statusReport.statusAudioRcvs.values()) {
                        audioBean = bean;
                    }
                    StatusBean videoBean = null;
                    for (StatusBean bean : statusReport.statusVideoRcvs.values()) {
                        videoBean = bean;
                    }
                    if (tvStatusReportView != null && videoBean != null) {
                        tvStatusReportView.setText("接收码率: " + statusReport.bitRateRcv + "kbps\n" + "码率: "
                                + (audioBean == null ? "0" : audioBean.bitRate) + "kbps \n" + "视频帧率: "
                                + (videoBean == null ? "0"
                                : videoBean.frameRate));
                    }
                }
            });
        }
    };

    private IRCRTCRoomEventsListener roomEventsListener = new IRCRTCRoomEventsListener() {

        /**
         * 房间内用户发布资源,直播模式下仅主播身份会执行该回调
         *
         * @param rcrtcRemoteUser 远端用户
         * @param list    发布的资源
         */
        @Override
        public void onRemoteUserPublishResource(RCRTCRemoteUser rcrtcRemoteUser, final List<RCRTCInputStream> list) {
            try {
                anchorStatus.subscribeAVStream();
                audienceStatus.subscribeAVStream();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRemoteUserMuteAudio(RCRTCRemoteUser rcrtcRemoteUser, RCRTCInputStream rcrtcInputStream, boolean b) {
        }

        @Override
        public void onRemoteUserMuteVideo(RCRTCRemoteUser rcrtcRemoteUser, RCRTCInputStream rcrtcInputStream, boolean b) {
        }

        @Override
        public void onRemoteUserUnpublishResource(RCRTCRemoteUser rcrtcRemoteUser, List<RCRTCInputStream> list) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<RCRTCVideoOutputStream> outputStreams = new ArrayList<>();
                        List<RCRTCVideoInputStream> inputStreams = new ArrayList<>();
                        getVideoStream(outputStreams, inputStreams);
                    }
                });
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        /**
         * 用户加入房间
         * @param rcrtcRemoteUser 远端用户
         */
        @Override
        public void onUserJoined(final RCRTCRemoteUser rcrtcRemoteUser) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toast("onUserJoined", Toast.LENGTH_LONG);
                    }
                });
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        /**
         * 用户离开房间
         * @param rcrtcRemoteUser 远端用户
         */
        @Override
        public void onUserLeft(RCRTCRemoteUser rcrtcRemoteUser) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toast("onUserLeft", Toast.LENGTH_LONG);
                        RCRTCVideoView rongRTCVideoView = flRemoteUser.findViewWithTag(LiveVideoView.class.getName());
                        // 远端用户离开时, videoview 在 mFlRemoteUser上，删除挂载在 mFlRemoteUser 上的 videoview
                        if (null != rongRTCVideoView) {
                            flRemoteUser.removeAllViews();
                            rongRTCVideoView = flFullscreen.findViewWithTag(LiveVideoView.class.getName());
                            // 远端用户离开时，如果本地预览正处于全屏状态自动退出全屏
                            if (rongRTCVideoView != null) {
                                flFullscreen.removeAllViews();
                                flLocalUser.addView(rongRTCVideoView);
                            }
                        } else {
                            // 远端用户离开时 , videoview 在 mFlFull 上，删除挂载在 mFlFull 上的 videoview
                            flFullscreen.removeAllViews();
                        }

                    }
                });
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUserOffline(RCRTCRemoteUser rcrtcRemoteUser) {
        }

        @Override
        public void onPublishLiveStreams(List<RCRTCInputStream> list) {
        }

        @Override
        public void onUnpublishLiveStreams(List<RCRTCInputStream> list) {
        }

        /**
         * 自己退出房间。 例如断网退出等
         * @param i 状态码
         */
        @Override
        public void onLeaveRoom(int i) {
            LiveActivity.this.finish();
        }

        /**
         * 观众端 接收自定义的房间属性消息
         * @param message
         */
        @Override
        public void onReceiveMessage(Message message) {
            super.onReceiveMessage(message);
            Log.e(TAG, "--> onReceiveMessage message = " + message);
            if (liveRoleType == RCRTCLiveRole.AUDIENCE.getType()) {
                MessageContent messageContent = message.getContent();
                if (messageContent instanceof RoomInfoMessage) {
                    RoomInfoMessage roomInfoMessage = (RoomInfoMessage) messageContent;
                    Log.e(TAG, "--> onReceiveMessage 观众端 接收自定义的房间属性消息 = " + roomInfoMessage.getUserName());
                    toast(roomInfoMessage.getUserName(), LENGTH_SHORT);
                }
            }

        }
    };

    private void release() {
        leaveRoom(new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LiveActivity.this.finish();
                    }
                });
            }

            @Override
            public void onFailed(RTCErrorCode rtcErrorCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LiveActivity.this.finish();
                    }
                });
            }
        });

    }

    public static void start(Context context, String roomId, String userId, int liveRoleType) {
        Intent intent = new Intent(context, LiveActivity.class);
        intent.putExtra(ROOM_ID, roomId);
        intent.putExtra(USER_ID, userId);
        intent.putExtra(LIVE_ROLE_TYPE, liveRoleType);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        context = LiveActivity.this;
        btnRequestLive = findViewById(R.id.btnRequestLive);
        btnCloseCamera = findViewById(R.id.btnCloseCamera);
        btnCloseMic = findViewById(R.id.btnCloseMic);
        btnSwitchCrame = findViewById(R.id.btnSwitchCrame);
        btnEndLive = findViewById(R.id.btnEndLive);
        flLocalUser = findViewById(R.id.flLocalUser);
        flRemoteUser = findViewById(R.id.flRemoteUser);
        flFullscreen = findViewById(R.id.flFullscreen);
        rgVideoStream = findViewById(R.id.rgVideoStream);
        rbTinyStream = findViewById(R.id.rbTinyStream);
        rbBigStream = findViewById(R.id.rbBigStream);
        tvStatusReportView = findViewById(R.id.tvStatusReportView);
        btnSetRoomProperties = findViewById(R.id.btnSetRoomProperties);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);

        btnRequestLive.setOnClickListener(this);
        btnCloseCamera.setOnClickListener(this);
        btnCloseMic.setOnClickListener(this);
        btnSwitchCrame.setOnClickListener(this);
        btnEndLive.setOnClickListener(this);
        btnSetRoomProperties.setOnClickListener(this);
        btnLeaveRoom.setOnClickListener(this);

        rgVideoStream.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RCRTCRemoteUser remoteUser = mRtcRoom.getRemoteUser(anchorUserId);
                switch (checkedId) {
                    case R.id.rbTinyStream:
                        // 切换为小流
                        if (remoteUser != null) {
                            remoteUser.switchToTinyStream(new IRCRTCResultCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.e(TAG, "--> 切换为小流 onSuccess");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("切为小流成功", LENGTH_SHORT);
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(RTCErrorCode errorCode) {
                                    Log.e(TAG, "--> 切换为小流 onFailed errorCode = " + errorCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("切为小流失败", LENGTH_SHORT);
                                        }
                                    });
                                }
                            });
                        } else {
                            Log.e(TAG, "--> 切换为小流 remoteUser 为空=" + remoteUser);
                        }
                        break;
                    case R.id.rbBigStream:
                        // 切换为大流
                        if (remoteUser != null) {
                            remoteUser.switchToNormalStream(new IRCRTCResultCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.e(TAG, "--> 切换为大流 onSuccess");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("切为大流成功", LENGTH_SHORT);
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(RTCErrorCode errorCode) {
                                    Log.e(TAG, "--> 切换为大流 onFailed errorCode = " + errorCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("切为大流失败", LENGTH_SHORT);
                                        }
                                    });
                                }
                            });
                        } else {
                            Log.e(TAG, "--> 切换为大流 remoteUser 为空=" + remoteUser);
                        }
                        break;
                }
            }
        });

        setIdleStatus();

        Intent intent = getIntent();
        roomId = intent.getStringExtra(ROOM_ID);
        userId = intent.getStringExtra(USER_ID);
        liveRoleType = intent.getIntExtra(LIVE_ROLE_TYPE, RCRTCLiveRole.BROADCASTER.getType());

        if (liveRoleType == RCRTCLiveRole.BROADCASTER.getType()) {
            setAnchorStatus();
        } else {
            setAudienceStatus();
        }
    }

    /**
     * 主播状态准备
     */
    void setAnchorStatus() {
        if (null != curStatus) {
            bakStatus = curStatus;
            // 恢复到 idle 状态
            setIdleStatus();
        }
        anchorStatus.config(context);
        anchorStatus.joinRoom(roomId);
    }

    /**
     * 观众状态准备
     */
    void setAudienceStatus() {
        if (null != curStatus) {
            bakStatus = curStatus;
            setIdleStatus();
        }
        audienceStatus.config(context);
        audienceStatus.joinRoom(roomId);
    }

    /**
     * 设置为空闲状态，在加入房间成功后设置为对应的主播或观众的状态
     */
    void setIdleStatus() {
        btnRequestLive.setVisibility(View.INVISIBLE);
        btnCloseCamera.setVisibility(View.INVISIBLE);
        btnCloseMic.setVisibility(View.INVISIBLE);
        btnSwitchCrame.setVisibility(View.INVISIBLE);
        btnEndLive.setVisibility(View.INVISIBLE);
        rgVideoStream.setVisibility(View.INVISIBLE);
        btnSetRoomProperties.setVisibility(View.GONE);
        tvStatusReportView.setVisibility(View.GONE);
        curStatus = idleStatus;
    }

    /**
     * 获得当前视频流
     * (2.调用 RCRTCLocalUser 下的 getStreams 方法获取主播推送的本地流，获得的视频流可用于本地的预览。)
     */
    public void getVideoStream(List<RCRTCVideoOutputStream> outputStreams, List<RCRTCVideoInputStream> inputStreams) {
        for (final RCRTCRemoteUser remoteUser : mRtcRoom.getRemoteUsers()) {
            if (remoteUser.getStreams().size() == 0) {
                continue;
            }
            List<RCRTCInputStream> userStreams = remoteUser.getStreams();
            for (RCRTCInputStream i : userStreams) {
                if (i.getMediaType() == RCRTCMediaType.VIDEO) {
                    inputStreams.add((RCRTCVideoInputStream) i);
                }
            }
        }

        for (RCRTCOutputStream o : mRtcRoom.getLocalUser().getStreams()) {
            if (o.getMediaType() == RCRTCMediaType.VIDEO) {
                outputStreams.add((RCRTCVideoOutputStream) o);
            }
        }
    }

    public void changeUi() {
        if (curStatus == anchorStatus) {//主播
            btnRequestLive.setVisibility(View.INVISIBLE);
            btnCloseCamera.setVisibility(View.VISIBLE);
            btnCloseMic.setVisibility(View.VISIBLE);
            btnSwitchCrame.setVisibility(View.VISIBLE);
            btnEndLive.setVisibility(View.VISIBLE);
            if (liveRoleType == BROADCASTER.getType()) {
                rgVideoStream.setVisibility(View.INVISIBLE);
                tvStatusReportView.setVisibility(View.GONE);
                btnSetRoomProperties.setVisibility(View.VISIBLE);
            } else {
                if (enableTinyStream && mRtcRoom.getRemoteUsers().size() > 0) {
                    rgVideoStream.setVisibility(View.VISIBLE);
                } else {
                    rgVideoStream.setVisibility(View.INVISIBLE);
                }
                tvStatusReportView.setVisibility(View.VISIBLE);
                btnSetRoomProperties.setVisibility(View.GONE);
            }
        } else {//观众
            btnRequestLive.setVisibility(View.VISIBLE);
            btnCloseCamera.setVisibility(View.INVISIBLE);
            btnCloseMic.setVisibility(View.INVISIBLE);
            btnSwitchCrame.setVisibility(View.INVISIBLE);
            btnEndLive.setVisibility(View.INVISIBLE);
            rgVideoStream.setVisibility(View.INVISIBLE);
            if (liveRoleType == RCRTCLiveRole.AUDIENCE.getType()) {
                tvStatusReportView.setVisibility(View.VISIBLE);
            }
            btnSetRoomProperties.setVisibility(View.GONE);
        }
    }

    public void leaveRoom(IRCRTCResultCallback ircrtcResultCallback) {
        if (null != mRtcRoom) {
            mRtcRoom.unregisterRoomListener();
        }
        RCRTCEngine.getInstance().leaveRoom(new IRCRTCResultCallback() {
            @Override
            public void onFailed(RTCErrorCode rtcErrorCode) {
                Log.e(TAG, "--> leaveRoom onFailed rtcErrorCode = " + rtcErrorCode);
                ircrtcResultCallback.onFailed(rtcErrorCode);
            }

            @Override
            public void onSuccess() {
                Log.e(TAG, "--> leaveRoom onSuccess ");
                // 删除房间属性
                if (liveRoleType == BROADCASTER.getType()) {//主播端
                    List<String> list = new ArrayList<>();
                    list.add(roomInfoMsgKey);
                    RoomInfoMessage roomInfoMessage = new RoomInfoMessage(roomInfoMsgKey, roomInfoMsgValue, RoomInfoMessage.JoinMode.AUDIO_VIDEO, 100002, false);
                    mRtcRoom.deleteRoomAttributes(list, roomInfoMessage, new IRCRTCResultCallback() {
                        @Override
                        public void onSuccess() {
                            Log.e(TAG, "--> leaveRoom 离开房间成功 删除房间属性 成功");
                        }

                        @Override
                        public void onFailed(RTCErrorCode rtcErrorCode) {
                            Log.e(TAG, "--> leaveRoom 离开房间成功 删除房间属性 失败 rtcErrorCode = " + rtcErrorCode);
                        }
                    });
                }
                ircrtcResultCallback.onSuccess();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnRequestLive) {
            if (liveRoleType == RCRTCLiveRole.AUDIENCE.getType()) {
                isAudienceRequestLive = true;
            }
            setAnchorStatus();
        } else if (view.getId() == R.id.btnEndLive) {
            leaveRoom(new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isAudienceRequestLive = false;
                            setAudienceStatus();
                        }
                    });

                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LiveActivity.this, "下麦失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });


        } else if (view.getId() == R.id.btnCloseCamera) {
            String str = ((Button) view).getText().toString();
            if (TextUtils.equals(str, AnchorConfig.CAMERA_STATUS_CLOSE)) {
                RCRTCEngine.getInstance().getDefaultVideoStream().stopCamera();
                ((Button) view).setText(AnchorConfig.CAMERA_STATUS_OPEN);
            } else {
                RCRTCEngine.getInstance().getDefaultVideoStream().startCamera(null);

                ((Button) view).setText(AnchorConfig.CAMERA_STATUS_CLOSE);
            }


        } else if (view.getId() == R.id.btnCloseMic) {
            String str = ((Button) view).getText().toString();
            if (TextUtils.equals(str, AnchorConfig.MIC_STATUS_CLOSE)) {
                RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(true);
                ((Button) view).setText(AnchorConfig.MIC_STATUS_OPEN);
            } else {
                RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(false);
                ((Button) view).setText(AnchorConfig.MIC_STATUS_CLOSE);
            }
        } else if (view.getId() == R.id.btnSwitchCrame) {
            RCRTCEngine.getInstance().getDefaultVideoStream().switchCamera(null);
        } else if (view.getId() == R.id.btnSetRoomProperties) {
            // 主播 设置房间属性
            RoomInfoMessage roomInfoMessage = new RoomInfoMessage(roomInfoMsgKey, roomInfoMsgValue, RoomInfoMessage.JoinMode.AUDIO_VIDEO, 100002, false);
//            Conversation.ConversationType conversationType = Conversation.ConversationType.PRIVATE;
//            Message message = Message.obtain(anchorUserId, conversationType, roomInfoMessage);
            mRtcRoom.setRoomAttribute(roomInfoMsgValue, roomInfoMsgKey, roomInfoMessage, new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "--> 主播加入房间 设置房间属性 成功");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("设置房间属性成功", LENGTH_SHORT);
                        }
                    });
                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    Log.e(TAG, "--> 主播加入房间 设置房间属性 失败 rtcErrorCode = " + rtcErrorCode);
                }
            });

        }else if (view.getId() == R.id.btnLeaveRoom){
            // 离开房间
            release();
        }
    }

    private void toast(String text, int lengthShort) {
        Toast.makeText(LiveActivity.this, text, lengthShort).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        flFullscreen.removeAllViews();
        flLocalUser.removeAllViews();
        flRemoteUser.removeAllViews();
        RCRTCEngine.getInstance().unInit();
        RCRTCEngine.getInstance().unregisterStatusReportListener();
    }

    class AnchorStatus implements IStatus {
        /**
         * 主播端 初始化及设置
         */
        @Override
        public void config(Context context) {
            RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
            // 是否硬解码
            configBuilder.enableHardwareDecoder(true);
            // 是否硬编码
            configBuilder.enableHardwareEncoder(true);
            // Texture 纹理类型为 RGB 视频格式的美颜
            configBuilder.enableEncoderTexture(true);
            // 设置断线后不自动重连
//            configBuilder.enableAutoReconnect(false);

            RCRTCEngine.getInstance().unInit();
            RCRTCEngine.getInstance().init(context, configBuilder.build());

            RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
            // 设置分辨率
            videoConfigBuilder.setVideoResolution(AnchorConfig.resolution);
            // 设置帧率
            videoConfigBuilder.setVideoFps(AnchorConfig.fps);
            /**
             * 设置最小码率，可根据分辨率RCRTCVideoResolution设置
             * {@link RCRTCParamsType.RCRTCVideoResolution)}
             */
            videoConfigBuilder.setMinRate(AnchorConfig.mixRate);
            /**
             * 设置最大码率，可根据分辨率RCRTCVideoResolution设置
             * {@link RCRTCParamsType.RCRTCVideoResolution)}
             */
            videoConfigBuilder.setMaxRate(AnchorConfig.maxRate);
            RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder.build());
            // 听筒播放，为避免噪音可在开发时设置为 false
            RCRTCEngine.getInstance().enableSpeaker(enableSpeaker);

        }

        /**
         * 主播加入房间
         */
        @Override
        public void joinRoom(String roomId) {
            RCRTCRoomConfig roomConfig = RCRTCRoomConfig.Builder.create()
                    // 根据实际场景，选择音视频直播：LIVE_AUDIO_VIDEO 或音频直播：LIVE_AUDIO
                    .setRoomType(RCRTCRoomType.LIVE_AUDIO_VIDEO)
                    .setLiveRole(BROADCASTER)
                    .build();

            // 美颜基础参数：基础参数目前包括：美白、磨皮、亮度、红润四个参数，取值范围为 [0-10]，0 代表无效果，10 代表最大效果。
            RCRTCBeautyOption beautyOption = RCRTCBeautyEngine.getInstance().getCurrentBeautyOption();
//            if (seekTypId == R.id.beauty_whiteness) {
                beautyOption.setWhitenessLevel(10);  // 设置美白参数
//            } else if (seekTypId == R.id.beauty_smooth) {
                beautyOption.setSmoothLevel(10);  // 设置磨皮参数
//            } else if (seekTypId == R.id.beauty_bright) {
                beautyOption.setBrightLevel(10);  // 设置亮度参数
//            } else if (seekTypId == R.id.beauty_ruddy) {
                beautyOption.setRuddyLevel(10);  // 设置红润参数
//            }
            RCRTCBeautyEngine.getInstance().setBeautyOption(true, beautyOption);  // true 是使用美颜，false 不使用美颜

            //美颜滤镜设置：滤镜目前包括：唯美、清新、浪漫三种风格，
//            RCRTCBeautyFilter beautyFilter = RCRTCBeautyEngine.getInstance().getCurrentFilter();
//            switch (checkedId){
//                case 0:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.NONE);  // 不使用美颜滤镜
//                    break;
//                }
//                case 1:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.ESTHETIC);  // 唯美
//                    break;
//                }
//                case 2:{
                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.FRESH);  // 清新
//                    break;
//                }
//                case 3:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.ROMANTIC);  // 浪漫
//                    break;
//                }
//                default:{
//                    Log.e(TAG, "onCheckedChanged: [group, checkedId]" + checkedId);
//                    break;
//                }
//            }


            RCRTCEngine.getInstance().joinRoom(roomId, roomConfig, new IRCRTCResultDataCallback<RCRTCRoom>() {
                @Override
                public void onSuccess(final RCRTCRoom rcrtcRoom) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRtcRoom = rcrtcRoom;
                            rcrtcRoom.registerRoomListener(roomEventsListener);
                            RCRTCEngine.getInstance().registerStatusReportListener(statusReportListener);
                            curStatus = anchorStatus;
                            changeUi();

                            RCRTCVideoView rongRTCVideoView = new LiveVideoView(getApplicationContext()) {};
                            RCRTCEngine.getInstance().getDefaultVideoStream().setVideoView(rongRTCVideoView);
                            flLocalUser.addView(rongRTCVideoView);
                            //TODO test
//                            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 180);
//                            flLocalUser.addView(rongRTCVideoView, layoutParams);
                            //todo

                            // 开始推流，本地用户发布
                            anchorStatus.publishDefaultAVStream();
                            // 主动订阅远端用户发布的资源
                            anchorStatus.subscribeAVStream();
                        }
                    });
                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            curStatus = bakStatus;
                            changeUi();
                        }
                    });
                }
            });
        }

        /**
         * 主播端 发布音视频流
         * 加入房间后，开始摄像头采集并发布音视频流。
         */
        @Override
        public void publishDefaultAVStream() {
            if (mRtcRoom == null) {
                return;
            }
            RCRTCEngine.getInstance().getDefaultVideoStream().startCamera(null);
            RCRTCEngine.getInstance().getDefaultVideoStream().enableTinyStream(enableTinyStream);
            mRtcRoom.getLocalUser().publishDefaultLiveStreams(new IRCRTCResultDataCallback<RCRTCLiveInfo>() {
                @Override
                public void onSuccess(RCRTCLiveInfo liveInfo) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<RCRTCVideoOutputStream> outputStreams = new ArrayList<>();
                                List<RCRTCVideoInputStream> inputStreams = new ArrayList<>();
                                getVideoStream(outputStreams, inputStreams);
                                RCRTCEngine.getInstance().getDefaultVideoStream().setVideoFrameListener(new IRCRTCVideoOutputFrameListener() {
                                    @Override
                                    public RCRTCVideoFrame processVideoFrame(RCRTCVideoFrame rcrtcVideoFrame) {

                                        return rcrtcVideoFrame;
                                    }
                                });
                            }
                        });
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    try {
                        Log.e(TAG, "--> publishDefaultAVStream onFailed rtcErrorCode = " + rtcErrorCode);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        /**
         * 主播端
         * 订阅远端用户资源(主播订阅房间内其他主播的资源)
         * 调用 RCRTCLocalUser 下的 subscribeStreams 方法订阅房间内其他主播的资源，在主播连麦的场景下会用到该方法，当远端主播取消发布资源时，会通过 onRemoteUserPublishResource() 回调通知，触发后需要处理订阅逻辑，比如取消显示的 view 视图。
         */
        @Override
        public void subscribeAVStream() {
            //TODO  示例代码，观众端收到视频大流
//            final List<RCRTCInputStream> inputStreams = mRtcRoom.getLiveStreams();
//            for (RCRTCInputStream inputStream : inputStreams) {
//                if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
//                    //选择订阅大流或是小流。
//                    ((RCRTCVideoInputStream) inputStream).setStreamType(RCRTCStreamType.NORMAL);
//                }
//            }
//            mRtcRoom.getLocalUser().subscribeStreams(inputStreams, new IRCRTCResultCallback() {
//                @Override
//                public void onSuccess() {
//                }
//
//                @Override
//                public void onFailed(RTCErrorCode errorCode) {
//                }
//            });


            if (mRtcRoom == null || mRtcRoom.getRemoteUsers() == null) {
                return;
            }
            List<RCRTCInputStream> subscribeInputStreams = new ArrayList<>();
            for (final RCRTCRemoteUser remoteUser : mRtcRoom.getRemoteUsers()) {
                if (remoteUser.getStreams().size() == 0) {
                    continue;
                }
                List<RCRTCInputStream> userStreams = remoteUser.getStreams();
                for (RCRTCInputStream inputStream : userStreams) {
                    if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
                        //选择订阅大流或是小流。
                        ((RCRTCVideoInputStream) inputStream).setStreamType(RCRTCStreamType.NORMAL);
                    }
                }
                subscribeInputStreams.addAll(userStreams);
            }

            if (subscribeInputStreams.size() == 0) {
                return;
            }
            mRtcRoom.getLocalUser().subscribeStreams(subscribeInputStreams, new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<RCRTCVideoOutputStream> outputStreams = new ArrayList<>();
                                List<RCRTCVideoInputStream> inputStreams = new ArrayList<>();
                                getVideoStream(outputStreams, inputStreams);

                                RCRTCVideoView videoView = new LiveVideoView(getApplicationContext()) {};
                                for (RCRTCInputStream inputStream : subscribeInputStreams) {
                                    if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
                                        ((RCRTCVideoInputStream) inputStream).setVideoView(videoView);
                                        // 将远端视图添加至布局
                                        LiveActivity.this.flRemoteUser.addView(videoView);
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    try {
                        Log.e(TAG, "--> subscribeAVStream onFailed errorCode = " + errorCode);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class AudienceStatus implements IStatus {

        /**
         * 观众端 初始化及设置
         */
        @Override
        public void config(Context context) {
            RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
            // 是否硬解码
            configBuilder.enableHardwareDecoder(true);
            // 是否硬编码
            configBuilder.enableHardwareEncoder(true);

            RCRTCEngine.getInstance().unInit();
            RCRTCEngine.getInstance().init(context, configBuilder.build());
        }

        /**
         * 观众端 加入房间
         *
         * @param roomId
         */
        @Override
        public void joinRoom(String roomId) {
            RCRTCRoomConfig roomConfig = RCRTCRoomConfig.Builder.create()
                    // 根据实际场景，选择音视频直播：LIVE_AUDIO_VIDEO 或音频直播：LIVE_AUDIO
                    .setRoomType(RCRTCRoomType.LIVE_AUDIO_VIDEO)
                    .setLiveRole(RCRTCLiveRole.AUDIENCE)
                    .build();
            RCRTCEngine.getInstance().joinRoom(roomId, roomConfig, new IRCRTCResultDataCallback<RCRTCRoom>() {
                @Override
                public void onSuccess(final RCRTCRoom rcrtcRoom) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mRtcRoom = rcrtcRoom;
//                                //TODO
//                                mRtcRoom.getRemoteUsers().get(0).getStreams();
//                                Log.e("直播","--> 观众加入房间成功 +mRtcRoom.getRemoteUsers().get(0).getStreams()="+mRtcRoom.getRemoteUsers().get(0).getStreams());
//                                mRtcRoom.getLiveStreams();
//                                Log.e("直播","--> 观众加入房间成功 +mRtcRoom.getLiveStreams()="+mRtcRoom.getLiveStreams());
//
//                                //todo

                                rcrtcRoom.registerRoomListener(roomEventsListener);
                                RCRTCEngine.getInstance().registerStatusReportListener(statusReportListener);
                                curStatus = audienceStatus;
                                changeUi();

                                RCRTCVideoView rongRTCVideoView = new LiveVideoView(getApplicationContext()) {};
                                RCRTCEngine.getInstance().getDefaultVideoStream().setVideoView(rongRTCVideoView);
                                flLocalUser.addView(rongRTCVideoView);

                                // 主动订阅
                                audienceStatus.subscribeAVStream();

                                // 获取房间属性
                                List<String> list = new ArrayList<>();
                                list.add(roomInfoMsgKey);
                                mRtcRoom.getRoomAttributes(list, new IRCRTCResultDataCallback<Map<String, String>>() {
                                    @Override
                                    public void onSuccess(Map<String, String> stringStringMap) {
                                        Log.e(TAG, "--> 观众端加入房间成功 获取房间属性 成功 stringStringMap.get(roomInfoMsgKey) = " + stringStringMap.get(roomInfoMsgKey));
                                    }

                                    @Override
                                    public void onFailed(RTCErrorCode rtcErrorCode) {
                                        Log.e(TAG, "--> 观众端加入房间成功 获取房间属性 失败 rtcErrorCode = " + rtcErrorCode);

                                    }
                                });
                            }
                        });

                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        curStatus = bakStatus;
                                        changeUi();
                                    }
                                });
                            }
                        });
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void publishDefaultAVStream() {

        }

        /**
         * 观众端 观看直播
         */
        @Override
        public void subscribeAVStream() {
            if (mRtcRoom == null || mRtcRoom.getRemoteUsers() == null) {
                return;
            }
            final List<RCRTCInputStream> inputStreams = new ArrayList<>();
            for (int i = 0; i < mRtcRoom.getRemoteUsers().size(); i++) {
                inputStreams.addAll(mRtcRoom.getRemoteUsers().get(0).getStreams());
                if (inputStreams.size() > 0) {
                    anchorUserId = mRtcRoom.getRemoteUsers().get(0).getUserId();
                    break;
                }
            }
            mRtcRoom.getLocalUser().subscribeStreams(inputStreams, new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (enableTinyStream && liveRoleType == RCRTCLiveRole.AUDIENCE.getType()) {
                                    if (isAudienceRequestLive) {
                                        rgVideoStream.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    rgVideoStream.setVisibility(View.INVISIBLE);
                                }
                                for (RCRTCInputStream inputStream : inputStreams) {
                                    if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
                                        inputStreams.add((RCRTCVideoInputStream) inputStream);
                                        break;
                                    }
                                }

                                //TODO
                                mInputStreamList = inputStreams;
                                //todo

                                RCRTCVideoView videoView = new LiveVideoView(getApplicationContext()) {};
                                for (RCRTCInputStream inputStream : inputStreams) {
                                    if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
                                        ((RCRTCVideoInputStream) inputStream).setVideoView(videoView);
                                        // 将远端视图添加至布局
                                        LiveActivity.this.flRemoteUser.addView(videoView);
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    try {
                        Log.e(TAG, "--> 观众端 subscribeAVStream onFailed errorCode = " + errorCode);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mInputStreamList != null){
            for (RCRTCInputStream inputStream : mInputStreamList){
                if (inputStream.getMediaType() == RCRTCMediaType.AUDIO){
                    inputStream.mute(true);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mInputStreamList != null){
            for (RCRTCInputStream inputStream : mInputStreamList){
                if (inputStream.getMediaType() == RCRTCMediaType.AUDIO){
                    inputStream.mute(false);
                }
            }
        }
    }

    /**
     * 继承RCRTCVideoView,可以重写RCRTCVideoView方法定制特殊需求，
     * 例如本例中重写onTouchEvent实现点击全屏
     */
    class LiveVideoView extends RCRTCVideoView {
        public LiveVideoView(Context context) {
            super(context);
            this.setTag(LiveVideoView.class.getName());
        }
    }
}
