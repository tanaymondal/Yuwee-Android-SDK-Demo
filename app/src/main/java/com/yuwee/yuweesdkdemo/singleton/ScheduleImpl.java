package com.yuwee.yuweesdkdemo.singleton;

import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnScheduleCallEventsListener;

import org.json.JSONObject;

import java.util.HashMap;

public class ScheduleImpl {
    private static final ScheduleImpl ourInstance = new ScheduleImpl();
    private HashMap<String, OnScheduleListener> hashMap = new HashMap<>();

    public static ScheduleImpl getInstance() {
        return ourInstance;
    }

    private ScheduleImpl() {
        setListener();
    }

    private void setListener() {
        Yuwee.getInstance().getCallManager().setOnScheduleCallEventsListener(new OnScheduleCallEventsListener() {
            @Override
            public void onNewScheduledCall(JSONObject jsonObject) {
                for (OnScheduleListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onNewScheduledCall(jsonObject);
                    }
                }
            }

            @Override
            public void onScheduledCallActivated(JSONObject jsonObject) {

                for (OnScheduleListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onScheduledCallActivated(jsonObject);
                    }
                }
            }

            @Override
            public void onScheduledCallDeleted(JSONObject jsonObject) {

                for (OnScheduleListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onScheduledCallDeleted(jsonObject);
                    }
                }
            }

            @Override
            public void onScheduleCallExpired(JSONObject jsonObject) {

                for (OnScheduleListener listener :
                        hashMap.values()) {
                    if (listener != null) {
                        listener.onScheduleCallExpired(jsonObject);
                    }
                }
            }
        });
    }

    public void setScheduleListener(OnScheduleListener listener) {
        hashMap.put(listener.getClass().getSimpleName(), listener);
    }

    public void removeScheduleListener(OnScheduleListener listener) {
        hashMap.remove(listener.getClass().getSimpleName());
    }

    public interface OnScheduleListener {
        void onNewScheduledCall(JSONObject jsonObject);

        void onScheduledCallActivated(JSONObject jsonObject);

        void onScheduledCallDeleted(JSONObject jsonObject);

        void onScheduleCallExpired(JSONObject jsonObject);
    }
}
