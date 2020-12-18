package com.yuwee.yuweesdkdemo.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityIncomingCallBinding;
import com.yuwee.yuweesdkdemo.singleton.CallImpl;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;

import org.json.JSONException;
import org.json.JSONObject;

public class IncomingCallActivity extends AppCompatActivity implements CallImpl.CallEndListener {

    public static final String DATA = "data";

    private ActivityIncomingCallBinding viewBinding = null;
    private JSONObject callData = null;
    private final CountDownTimer timer = new CountDownTimer(20000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            new Handler(Looper.getMainLooper()).post(() -> {
                Yuwee.getInstance().getCallManager().timeOutIncomingCall();
                finish();
                Utils.showToast("Call timed out!");
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_incoming_call);


        CallImpl.getInstance().setCallEndListener(this);

        try {
            callData = new JSONObject(getIntent().getExtras().getString(DATA, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        timer.start();
        viewBinding.tvCallType.setText("Incoming " + (callData.optBoolean("isGroup") ? "Group" : "P2P") + " call");
        viewBinding.tvEmail.setText(callData.optJSONObject("sender").optString("email"));
        viewBinding.tvName.setText(callData.optJSONObject("sender").optString("name"));

        Yuwee.getInstance().getCallManager().notifyRinging();
    }

    public void onReject(View view) {
        timer.cancel();
        Yuwee.getInstance().getCallManager().rejectIncomingCall();
        finish();
    }

    public void onAccept(View view) {
        timer.cancel();
        Yuwee.getInstance().getCallManager().acceptIncomingCall();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallImpl.getInstance().removeCallEndListener(this);
    }

    @Override
    public void onCallEnded(JSONObject jsonObject) {
        Utils.showToast("Call ended!");
        timer.cancel();
        finish();
    }

    @Override
    public void onCallRejected() {

    }

    @Override
    public void onCallTimeout() {

    }
}
