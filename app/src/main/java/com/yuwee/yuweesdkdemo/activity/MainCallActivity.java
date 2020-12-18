package com.yuwee.yuweesdkdemo.activity;


import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityMainCallBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutAddMemberBinding;
import com.yuwee.yuweesdkdemo.singleton.CallImpl;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.YuweeCallActivity;
import com.yuwee.sdk.enums.AudioOutputType;
import com.yuwee.sdk.listener.CallHangupEventListener;
import com.yuwee.sdk.listener.OnAddMemberOnCallListener;
import com.yuwee.sdk.listener.OnScreenSharingListener;
import com.yuwee.sdk.model.CallConfig;
import com.yuwee.sdk.model.group_update.Member;
import com.yuwee.sdk.view.YuweeVideoView;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainCallActivity extends YuweeCallActivity implements CallImpl.CallEndListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private ActivityMainCallBinding viewBinding = null;
    private boolean isScreenSharingActive = false;
    public static final String MEDIA_TYPE = "mediaType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main_call);

        CallConfig callConfig = new CallConfig();
        callConfig.setAudioSampleRate(16);
        Yuwee.getInstance().getCallManager().init(callConfig);

        if (getIntent().hasExtra(MEDIA_TYPE)) {
            String mediaType = getIntent().getExtras().getString(MEDIA_TYPE, "");
            if (mediaType.equalsIgnoreCase("AUDIO")) {
                Yuwee.getInstance().getCallManager().setVideoEnabled(false);
                Yuwee.getInstance().getCallManager().stopVideoSource();
            }
        }

        callHangupListener();
        CallImpl.getInstance().setCallEndListener(this);
        initClicks();

        setSupportActionBar(viewBinding.toolbar);
    }

    private void initClicks() {
        viewBinding.incControl.audio.setOnCheckedChangeListener(this);
        viewBinding.incControl.speaker.setOnCheckedChangeListener(this);
        viewBinding.incControl.video.setOnCheckedChangeListener(this);

        viewBinding.incControl.ivEnd.setOnClickListener(this);
        viewBinding.incControl.ivPlus.setOnClickListener(this);
    }

    @Override
    protected YuweeVideoView setRemoteVideoView() {
        return viewBinding.videoViewRemote;
    }

    @Override
    protected YuweeVideoView setLocalVideoView() {
        return viewBinding.videoViewLocal;
    }

    private void callHangupListener() {
        Yuwee.getInstance().getCallManager().setCallHangupEventListener(new CallHangupEventListener() {
            @Override
            public void onCallHangUpSuccess() {
                DialogUtils.cancelDialog(MainCallActivity.this);
                Utils.showToast("Call ended!");
                finish();
            }

            @Override
            public void onError(String s) {
                DialogUtils.cancelDialog(MainCallActivity.this);
                Utils.showToast("An error occurred to end call!");
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_switch_camera:
                Yuwee.getInstance().getCallManager().switchCamera();
                break;

            case R.id.menu_switch_stream:
                Yuwee.getInstance().getCallManager().switchRemoteStream();
                break;

            case R.id.menu_start_screen_sharing:
                screenSharing();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.audio:
                Yuwee.getInstance().getCallManager().setAudioEnabled(!isChecked);
                break;
            case R.id.speaker:
                if (isChecked) {
                    Yuwee.getInstance().getCallManager().setAudioOutputType(AudioOutputType.EARPIECE);
                } else {
                    Yuwee.getInstance().getCallManager().setAudioOutputType(AudioOutputType.SPEAKER);
                }
                break;

            case R.id.video:
                Yuwee.getInstance().getCallManager().setVideoEnabled(!isChecked);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_end:
                DialogUtils.showDialog(this, "Hanging up...");
                Yuwee.getInstance().getCallManager().hangUpCall();
                break;
            case R.id.iv_plus:
                addMember();
                break;
        }
    }

    private void addMember() {

        LayoutAddMemberBinding viewBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_add_member, null, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(viewBinding.getRoot());
        builder.setPositiveButton("Add", (dialog, which) -> {
            if (TextUtils.isEmpty(viewBinding.etGroupName.getText().toString().trim())) {
                Utils.showToast("Please enter group name.");
                return;
            }
            if (viewBinding.etGroupName.getText().toString().trim().length() < 3) {
                Utils.showToast("Group name must be greater than 3.");
                return;
            }
            if (TextUtils.isEmpty(viewBinding.etEmail.getText().toString().trim()) ||
                    !Patterns.EMAIL_ADDRESS.matcher(viewBinding.etEmail.getText().toString().trim()).matches()) {
                Utils.showToast("Please enter valid email.");
                return;
            }
            addMember(viewBinding.etGroupName.getText().toString().trim(), viewBinding.etEmail.getText().toString().trim());

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });
        builder.show();


    }

    private void addMember(String groupName, String email) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(email);

        DialogUtils.showDialog(this, "Adding member...");
        Yuwee.getInstance().getCallManager().addMemberOnCall(arrayList, groupName, new OnAddMemberOnCallListener() {
            @Override
            public void onAddMemberSuccess(ArrayList<Member> memberList) {
                DialogUtils.cancelDialog(MainCallActivity.this);
            }

            @Override
            public void onAddMemberFailure(String message) {
                DialogUtils.cancelDialog(MainCallActivity.this);
            }
        });
    }

    public void enableHeadSet(View view) {
        Yuwee.getInstance().getCallManager().setAudioOutputType(AudioOutputType.WIRED_HEADSET);
    }

    private void screenSharing() {
        if (isScreenSharingActive) {
            Yuwee.getInstance().getCallManager().stopScreenSharing();
            isScreenSharingActive = false;
        } else {
            Yuwee.getInstance().getCallManager().startScreenSharing(new OnScreenSharingListener() {
                @Override
                public void onScreenSharingSuccess() {
                    isScreenSharingActive = true;
                    Log.e("TANAY", "onScreenSharingSuccess");
                }

                @Override
                public void onScreenSharingError(String error) {
                    Log.e("TANAY", "onScreenSharingError");
                }
            });
        }

    }

    @Override
    public void onCallEnded(JSONObject jsonObject) {
        Utils.showToast("Call ended!");
        finish();
    }

    @Override
    public void onCallRejected() {
        Utils.showToast("Call rejected!");
        finish();
    }

    @Override
    public void onCallTimeout() {
        Utils.showToast("User didn't pick up the call!");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("TANAY", "Main Call Activity Destroy");
        CallImpl.getInstance().removeCallEndListener(this);
    }
}