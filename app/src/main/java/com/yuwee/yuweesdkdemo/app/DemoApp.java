package com.yuwee.yuweesdkdemo.app;

import android.content.Intent;

import androidx.multidex.MultiDexApplication;

import com.yuwee.yuweesdkdemo.activity.IncomingCallActivity;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.enums.YuweeMode;
import com.yuwee.sdk.listener.OnYuWeeNotificationListener;

import org.json.JSONObject;

import java.util.Map;


public class DemoApp extends MultiDexApplication {

    private static DemoApp instance = null;

    public static DemoApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;



        Yuwee.getInstance()
                .init(this,
                        Constants.appId,
                        Constants.appSecret,
                        Constants.clientId);

    }

    public void processNotification(Map<String, String> data) {
        Yuwee.getInstance().getNotificationManager().process(data, new OnYuWeeNotificationListener() {
            @Override
            public void onReceiveCallFromPush(JSONObject callData) {
                Utils.log("onReceiveCallFromPush");
                if (!Yuwee.getInstance().getConnectionManager().isConnected()) {
                    Intent intent = new Intent(DemoApp.getInstance(), IncomingCallActivity.class);
                    intent.putExtra(IncomingCallActivity.DATA, callData.toString());
                    //intent.putExtra(IncomingCallActivity.EMAIL, email);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onNewScheduleMeetingFromPush(JSONObject jsonObject) {
                Utils.log("onNewScheduleMeetingFromPush");
            }

            @Override
            public void onScheduleMeetingJoinFromPush(JSONObject jsonObject) {
                Utils.log("onScheduleMeetingJoinFromPush");
            }

            @Override
            public void onChatMessageReceivedFromPush(JSONObject jsonObject) {
                Utils.log("onChatMessageReceivedFromPush");
                Utils.showTextNotification(jsonObject);
            }
        });
    }
}
