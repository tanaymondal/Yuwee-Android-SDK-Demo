package com.yuwee.yuweesdkdemo.fcm;


import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.yuwee.yuweesdkdemo.app.DemoApp;
import com.yuwee.yuweesdkdemo.utils.Utils;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Utils.log("Push Message: " + remoteMessage.getData().toString());

        DemoApp.getInstance().processNotification(remoteMessage.getData());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Utils.log("Token: " + s);
    }

}
