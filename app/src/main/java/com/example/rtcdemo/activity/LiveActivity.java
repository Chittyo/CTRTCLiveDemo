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
    private int liveRoleType = BROADCASTER.getType();// 1:??????, 2:??????
    private Button btnRequestLive, btnCloseCamera, btnCloseMic, btnSwitchCrame, btnEndLive, btnSetRoomProperties, btnLeaveRoom;
    private FrameLayout flLocalUser, flRemoteUser, flFullscreen;
    private RadioGroup rgVideoStream;
    private RadioButton rbTinyStream, rbBigStream;
    private TextView tvStatusReportView;
    private RCRTCRoom mRtcRoom = null;
    private boolean enableTinyStream = false;// true:???????????????false:???????????????
    private boolean isAudienceRequestLive = false;//????????????
    private String roomInfoMsgKey = "weatherForecast";
    private String roomInfoMsgValue = "????????????";
    private String anchorUserId;

    IStatus idleStatus = new IdleStatus();
    IStatus anchorStatus = new AnchorStatus();
    IStatus audienceStatus = new AudienceStatus();

    IStatus curStatus;// ??????????????????????????????????????????????????????
    IStatus bakStatus;// ??????????????????????????????????????????????????????

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
                        tvStatusReportView.setText("????????????: " + statusReport.bitRateRcv + "kbps\n" + "??????: "
                                + (audioBean == null ? "0" : audioBean.bitRate) + "kbps \n" + "????????????: "
                                + (videoBean == null ? "0"
                                : videoBean.frameRate));
                    }
                }
            });
        }
    };

    private IRCRTCRoomEventsListener roomEventsListener = new IRCRTCRoomEventsListener() {

        /**
         * ???????????????????????????,????????????????????????????????????????????????
         *
         * @param rcrtcRemoteUser ????????????
         * @param list    ???????????????
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
         * ??????????????????
         * @param rcrtcRemoteUser ????????????
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
         * ??????????????????
         * @param rcrtcRemoteUser ????????????
         */
        @Override
        public void onUserLeft(RCRTCRemoteUser rcrtcRemoteUser) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toast("onUserLeft", Toast.LENGTH_LONG);
                        RCRTCVideoView rongRTCVideoView = flRemoteUser.findViewWithTag(LiveVideoView.class.getName());
                        // ?????????????????????, videoview ??? mFlRemoteUser????????????????????? mFlRemoteUser ?????? videoview
                        if (null != rongRTCVideoView) {
                            flRemoteUser.removeAllViews();
                            rongRTCVideoView = flFullscreen.findViewWithTag(LiveVideoView.class.getName());
                            // ?????????????????????????????????????????????????????????????????????????????????
                            if (rongRTCVideoView != null) {
                                flFullscreen.removeAllViews();
                                flLocalUser.addView(rongRTCVideoView);
                            }
                        } else {
                            // ????????????????????? , videoview ??? mFlFull ????????????????????? mFlFull ?????? videoview
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
         * ????????????????????? ?????????????????????
         * @param i ?????????
         */
        @Override
        public void onLeaveRoom(int i) {
            LiveActivity.this.finish();
        }

        /**
         * ????????? ????????????????????????????????????
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
                    Log.e(TAG, "--> onReceiveMessage ????????? ???????????????????????????????????? = " + roomInfoMessage.getUserName());
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
                        // ???????????????
                        if (remoteUser != null) {
                            remoteUser.switchToTinyStream(new IRCRTCResultCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.e(TAG, "--> ??????????????? onSuccess");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("??????????????????", LENGTH_SHORT);
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(RTCErrorCode errorCode) {
                                    Log.e(TAG, "--> ??????????????? onFailed errorCode = " + errorCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("??????????????????", LENGTH_SHORT);
                                        }
                                    });
                                }
                            });
                        } else {
                            Log.e(TAG, "--> ??????????????? remoteUser ??????=" + remoteUser);
                        }
                        break;
                    case R.id.rbBigStream:
                        // ???????????????
                        if (remoteUser != null) {
                            remoteUser.switchToNormalStream(new IRCRTCResultCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.e(TAG, "--> ??????????????? onSuccess");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("??????????????????", LENGTH_SHORT);
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(RTCErrorCode errorCode) {
                                    Log.e(TAG, "--> ??????????????? onFailed errorCode = " + errorCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("??????????????????", LENGTH_SHORT);
                                        }
                                    });
                                }
                            });
                        } else {
                            Log.e(TAG, "--> ??????????????? remoteUser ??????=" + remoteUser);
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
     * ??????????????????
     */
    void setAnchorStatus() {
        if (null != curStatus) {
            bakStatus = curStatus;
            // ????????? idle ??????
            setIdleStatus();
        }
        anchorStatus.config(context);
        anchorStatus.joinRoom(roomId);
    }

    /**
     * ??????????????????
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
     * ??????????????????????????????????????????????????????????????????????????????????????????
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
     * ?????????????????????
     * (2.?????? RCRTCLocalUser ?????? getStreams ????????????????????????????????????????????????????????????????????????????????????)
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
        if (curStatus == anchorStatus) {//??????
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
        } else {//??????
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
                // ??????????????????
                if (liveRoleType == BROADCASTER.getType()) {//?????????
                    List<String> list = new ArrayList<>();
                    list.add(roomInfoMsgKey);
                    RoomInfoMessage roomInfoMessage = new RoomInfoMessage(roomInfoMsgKey, roomInfoMsgValue, RoomInfoMessage.JoinMode.AUDIO_VIDEO, 100002, false);
                    mRtcRoom.deleteRoomAttributes(list, roomInfoMessage, new IRCRTCResultCallback() {
                        @Override
                        public void onSuccess() {
                            Log.e(TAG, "--> leaveRoom ?????????????????? ?????????????????? ??????");
                        }

                        @Override
                        public void onFailed(RTCErrorCode rtcErrorCode) {
                            Log.e(TAG, "--> leaveRoom ?????????????????? ?????????????????? ?????? rtcErrorCode = " + rtcErrorCode);
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
                            Toast.makeText(LiveActivity.this, "????????????", Toast.LENGTH_SHORT).show();
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
            // ?????? ??????????????????
            RoomInfoMessage roomInfoMessage = new RoomInfoMessage(roomInfoMsgKey, roomInfoMsgValue, RoomInfoMessage.JoinMode.AUDIO_VIDEO, 100002, false);
//            Conversation.ConversationType conversationType = Conversation.ConversationType.PRIVATE;
//            Message message = Message.obtain(anchorUserId, conversationType, roomInfoMessage);
            mRtcRoom.setRoomAttribute(roomInfoMsgValue, roomInfoMsgKey, roomInfoMessage, new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "--> ?????????????????? ?????????????????? ??????");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("????????????????????????", LENGTH_SHORT);
                        }
                    });
                }

                @Override
                public void onFailed(RTCErrorCode rtcErrorCode) {
                    Log.e(TAG, "--> ?????????????????? ?????????????????? ?????? rtcErrorCode = " + rtcErrorCode);
                }
            });

        }else if (view.getId() == R.id.btnLeaveRoom){
            // ????????????
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
         * ????????? ??????????????????
         */
        @Override
        public void config(Context context) {
            RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
            // ???????????????
            configBuilder.enableHardwareDecoder(true);
            // ???????????????
            configBuilder.enableHardwareEncoder(true);
            // Texture ??????????????? RGB ?????????????????????
            configBuilder.enableEncoderTexture(true);
            // ??????????????????????????????
//            configBuilder.enableAutoReconnect(false);

            RCRTCEngine.getInstance().unInit();
            RCRTCEngine.getInstance().init(context, configBuilder.build());

            RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
            // ???????????????
            videoConfigBuilder.setVideoResolution(AnchorConfig.resolution);
            // ????????????
            videoConfigBuilder.setVideoFps(AnchorConfig.fps);
            /**
             * ???????????????????????????????????????RCRTCVideoResolution??????
             * {@link RCRTCParamsType.RCRTCVideoResolution)}
             */
            videoConfigBuilder.setMinRate(AnchorConfig.mixRate);
            /**
             * ???????????????????????????????????????RCRTCVideoResolution??????
             * {@link RCRTCParamsType.RCRTCVideoResolution)}
             */
            videoConfigBuilder.setMaxRate(AnchorConfig.maxRate);
            RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder.build());
            // ?????????????????????????????????????????????????????? false
            RCRTCEngine.getInstance().enableSpeaker(enableSpeaker);

        }

        /**
         * ??????????????????
         */
        @Override
        public void joinRoom(String roomId) {
            RCRTCRoomConfig roomConfig = RCRTCRoomConfig.Builder.create()
                    // ?????????????????????????????????????????????LIVE_AUDIO_VIDEO ??????????????????LIVE_AUDIO
                    .setRoomType(RCRTCRoomType.LIVE_AUDIO_VIDEO)
                    .setLiveRole(BROADCASTER)
                    .build();

            // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????? [0-10]???0 ??????????????????10 ?????????????????????
            RCRTCBeautyOption beautyOption = RCRTCBeautyEngine.getInstance().getCurrentBeautyOption();
//            if (seekTypId == R.id.beauty_whiteness) {
                beautyOption.setWhitenessLevel(10);  // ??????????????????
//            } else if (seekTypId == R.id.beauty_smooth) {
                beautyOption.setSmoothLevel(10);  // ??????????????????
//            } else if (seekTypId == R.id.beauty_bright) {
                beautyOption.setBrightLevel(10);  // ??????????????????
//            } else if (seekTypId == R.id.beauty_ruddy) {
                beautyOption.setRuddyLevel(10);  // ??????????????????
//            }
            RCRTCBeautyEngine.getInstance().setBeautyOption(true, beautyOption);  // true ??????????????????false ???????????????

            //?????????????????????????????????????????????????????????????????????????????????
//            RCRTCBeautyFilter beautyFilter = RCRTCBeautyEngine.getInstance().getCurrentFilter();
//            switch (checkedId){
//                case 0:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.NONE);  // ?????????????????????
//                    break;
//                }
//                case 1:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.ESTHETIC);  // ??????
//                    break;
//                }
//                case 2:{
                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.FRESH);  // ??????
//                    break;
//                }
//                case 3:{
//                    RCRTCBeautyEngine.getInstance().setBeautyFilter(RCRTCBeautyFilter.ROMANTIC);  // ??????
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

                            // ?????????????????????????????????
                            anchorStatus.publishDefaultAVStream();
                            // ???????????????????????????????????????
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
         * ????????? ??????????????????
         * ???????????????????????????????????????????????????????????????
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
         * ?????????
         * ????????????????????????(??????????????????????????????????????????)
         * ?????? RCRTCLocalUser ?????? subscribeStreams ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? onRemoteUserPublishResource() ???????????????????????????????????????????????????????????????????????? view ?????????
         */
        @Override
        public void subscribeAVStream() {
            //TODO  ??????????????????????????????????????????
//            final List<RCRTCInputStream> inputStreams = mRtcRoom.getLiveStreams();
//            for (RCRTCInputStream inputStream : inputStreams) {
//                if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
//                    //?????????????????????????????????
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
                        //?????????????????????????????????
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
                                        // ??????????????????????????????
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
         * ????????? ??????????????????
         */
        @Override
        public void config(Context context) {
            RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
            // ???????????????
            configBuilder.enableHardwareDecoder(true);
            // ???????????????
            configBuilder.enableHardwareEncoder(true);

            RCRTCEngine.getInstance().unInit();
            RCRTCEngine.getInstance().init(context, configBuilder.build());
        }

        /**
         * ????????? ????????????
         *
         * @param roomId
         */
        @Override
        public void joinRoom(String roomId) {
            RCRTCRoomConfig roomConfig = RCRTCRoomConfig.Builder.create()
                    // ?????????????????????????????????????????????LIVE_AUDIO_VIDEO ??????????????????LIVE_AUDIO
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
//                                Log.e("??????","--> ???????????????????????? +mRtcRoom.getRemoteUsers().get(0).getStreams()="+mRtcRoom.getRemoteUsers().get(0).getStreams());
//                                mRtcRoom.getLiveStreams();
//                                Log.e("??????","--> ???????????????????????? +mRtcRoom.getLiveStreams()="+mRtcRoom.getLiveStreams());
//
//                                //todo

                                rcrtcRoom.registerRoomListener(roomEventsListener);
                                RCRTCEngine.getInstance().registerStatusReportListener(statusReportListener);
                                curStatus = audienceStatus;
                                changeUi();

                                RCRTCVideoView rongRTCVideoView = new LiveVideoView(getApplicationContext()) {};
                                RCRTCEngine.getInstance().getDefaultVideoStream().setVideoView(rongRTCVideoView);
                                flLocalUser.addView(rongRTCVideoView);

                                // ????????????
                                audienceStatus.subscribeAVStream();

                                // ??????????????????
                                List<String> list = new ArrayList<>();
                                list.add(roomInfoMsgKey);
                                mRtcRoom.getRoomAttributes(list, new IRCRTCResultDataCallback<Map<String, String>>() {
                                    @Override
                                    public void onSuccess(Map<String, String> stringStringMap) {
                                        Log.e(TAG, "--> ??????????????????????????? ?????????????????? ?????? stringStringMap.get(roomInfoMsgKey) = " + stringStringMap.get(roomInfoMsgKey));
                                    }

                                    @Override
                                    public void onFailed(RTCErrorCode rtcErrorCode) {
                                        Log.e(TAG, "--> ??????????????????????????? ?????????????????? ?????? rtcErrorCode = " + rtcErrorCode);

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
         * ????????? ????????????
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
                                        // ??????????????????????????????
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
                        Log.e(TAG, "--> ????????? subscribeAVStream onFailed errorCode = " + errorCode);
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
     * ??????RCRTCVideoView,????????????RCRTCVideoView???????????????????????????
     * ?????????????????????onTouchEvent??????????????????
     */
    class LiveVideoView extends RCRTCVideoView {
        public LiveVideoView(Context context) {
            super(context);
            this.setTag(LiveVideoView.class.getName());
        }
    }
}
