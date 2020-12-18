package com.yuwee.yuweesdkdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.yuwee.yuweesdkdemo.activity.MeetingCallActivity;

public class DemoService extends Service {
    public DemoService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case MeetingCallActivity.SHOW_ONGOING_CALL_NOTIFICATION: {
                    startForeground(1001, MeetingCallActivity.showOngoingCallNotification());
                }
                break;
                case MeetingCallActivity.STOP_ONGOING_CALL_NOTIFICATION: {
                    stopForeground(true);
                }
            }
        }

        return START_NOT_STICKY;
    }
}
