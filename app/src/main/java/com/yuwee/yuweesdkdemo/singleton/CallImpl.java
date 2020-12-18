package com.yuwee.yuweesdkdemo.singleton;

import android.content.Intent;

import com.yuwee.yuweesdkdemo.activity.IncomingCallActivity;
import com.yuwee.yuweesdkdemo.activity.MainCallActivity;
import com.yuwee.yuweesdkdemo.app.DemoApp;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnCallEventListener;
import com.yuwee.sdk.listener.OnIncomingCallEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class CallImpl {
    private static final CallImpl ourInstance = new CallImpl();
    private HashMap<String, CallEndListener> hashMap = new HashMap<>();

    public static CallImpl getInstance() {
        return ourInstance;
    }

    private CallImpl() {
    }

    public void initListener(){
        setIncomingCallListener();
        setUpCallListener();
    }

    String mediaType = "";
    private void setIncomingCallListener() {
        Yuwee.getInstance().getCallManager().setOnIncomingCallEventListener(new OnIncomingCallEventListener() {
            @Override
            public void onIncomingCall(JSONObject callData) {

                JSONObject msgOBJ;
                try {
                    msgOBJ = new JSONObject(callData.optString("message"));
                    mediaType = msgOBJ.optString("callType", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(DemoApp.getInstance(), IncomingCallActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(IncomingCallActivity.DATA, callData.toString());
                DemoApp.getInstance().startActivity(intent);
            }

            @Override
            public void onIncomingCallAcceptSuccess() {
                Intent intent = new Intent(DemoApp.getInstance(), MainCallActivity.class);
                intent.putExtra(MainCallActivity.MEDIA_TYPE, mediaType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                DemoApp.getInstance().startActivity(intent);
            }

            @Override
            public void onIncomingCallRejectSuccess() {
                Utils.showToast("Call Rejected");
            }
        });
    }

    private void setUpCallListener() {
        Yuwee.getInstance().getCallManager().setCallEventListener(new OnCallEventListener() {
            @Override
            public void onCallRinging() {
                Utils.log("onCallRinging");
            }

            @Override
            public void onCallTimeOut() {
                Utils.log("onCallTimeOut");
                for (CallEndListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onCallTimeout();
                    }
                }
            }

            @Override
            public void onCallAccept() {
                Utils.log("onCallAccept");
            }

            @Override
            public void onRemoteCallHangUp(JSONObject jsonObject) {
                Utils.log("onRemoteCallHangUp");
            }

            @Override
            public void onCallReject() {
                Utils.log("onCallReject");
                for (CallEndListener listener : hashMap.values()) {
                    if (listener != null) {
                        listener.onCallRejected();
                    }
                }
            }

            @Override
            public void onCallConnected() {
                Utils.log("onCallConnected");
            }

            @Override
            public void onCallReconnecting() {
                Utils.log("onCallReconnecting");
            }

            @Override
            public void onCallReconnected() {
                Utils.log("onCallReconnected");
            }

            @Override
            public void onCallEnd(JSONObject jsonObject) {
                Utils.log("onCallEnd socket " + jsonObject);

                for (CallEndListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onCallEnded(jsonObject);
                    }
                }
            }

            @Override
            public void onCallConnectionFailed() {
                Utils.log("onCallConnectionFailed");
            }

            @Override
            public void onCallDisconnected() {
                Utils.log("onCallDisconnected");
            }

            @Override
            public void onCallReconnectionFailed() {
                Utils.log("onCallReconnectionFailed");
            }
        });
    }

    public void removeCallEndListener(CallEndListener listener) {
        hashMap.remove(listener.getClass().getSimpleName());
    }

    public void setCallEndListener(CallEndListener listener) {
        hashMap.put(listener.getClass().getSimpleName(), listener);
    }

    public interface CallEndListener {
        void onCallEnded(JSONObject jsonObject);
        void onCallRejected();
        void onCallTimeout();
    }


}
