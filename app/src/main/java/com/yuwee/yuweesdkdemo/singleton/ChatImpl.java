package com.yuwee.yuweesdkdemo.singleton;

import com.yuwee.sdk.Yuwee;

import org.json.JSONObject;

import java.util.HashMap;

public class ChatImpl {
    private static final ChatImpl ourInstance = new ChatImpl();
    private HashMap<String, ChatListener> listenerHashMap = new HashMap<>();
    private HashMap<String, UpdateCountListener> listenerCountMap = new HashMap<>();

    public static ChatImpl getInstance() {
        return ourInstance;
    }

    private ChatImpl() {
        init();
    }

    private void init() {
        Yuwee.getInstance().getChatManager().setMessageDeliveryListener(jsonObject -> {
            for (ChatListener listener : listenerHashMap.values()) {
                if (listener != null) {
                    listener.onMessageDeliverySuccess(jsonObject);
                }
            }
        });


        Yuwee.getInstance().getChatManager().setOnMessageDeletedListener(jsonObject -> {
            for (ChatListener listener : listenerHashMap.values()) {
                if (listener != null) {
                    listener.onMessageDeleted(jsonObject);
                }
            }
        });

        Yuwee.getInstance().getChatManager().setOnTypingEventListener(jsonObject -> {
            for (ChatListener listener : listenerHashMap.values()) {
                if (listener != null) {
                    listener.onUserTypingInRoom(jsonObject);
                }
            }
        });

        Yuwee.getInstance().getChatManager().setOnNewMessageReceivedListener(jsonObject -> {
            for (ChatListener listener : listenerHashMap.values()) {
                if (listener != null) {
                    listener.onNewMessageReceived(jsonObject);
                }
            }
        });
    }


    public void setChatListener(ChatListener listener) {
        listenerHashMap.put(listener.getClass().getSimpleName(), listener);
    }

    public void removeChatListener(ChatListener listener) {
        listenerHashMap.remove(listener.getClass().getSimpleName());
    }

    public void setUpdateCountListener(UpdateCountListener listener) {
        listenerCountMap.put(listener.getClass().getSimpleName(), listener);
    }

    public void removeUpdateCountListener(UpdateCountListener listener) {
        listenerCountMap.remove(listener.getClass().getSimpleName());
    }

    public void updateCount(String roomId) {

        for (UpdateCountListener listener : listenerCountMap.values()) {
            if (listener != null) {
                listener.onClearCount(roomId);
            }
        }
    }


    public interface ChatListener {
        void onNewMessageReceived(JSONObject jsonObject);

        void onUserTypingInRoom(JSONObject jsonObject);

        void onMessageDeleted(JSONObject jsonObject);

        void onMessageDeliverySuccess(JSONObject jsonObject);
    }

    public interface UpdateCountListener {
        void onClearCount(String roomId);
    }

}
