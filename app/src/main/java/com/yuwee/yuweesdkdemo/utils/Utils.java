package com.yuwee.yuweesdkdemo.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.ChatDetailsActivity;
import com.yuwee.yuweesdkdemo.app.AppData;
import com.yuwee.yuweesdkdemo.app.DemoApp;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    public static void showToast(String message) {
        Toast.makeText(DemoApp.getInstance().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void log(Object o) {
        Log.e("TANAY", o.toString());
    }

    public static String formatTime(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy hh:mm a");
        return simpleDateFormat.format(new Date(Long.parseLong(time)));
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) DemoApp.getInstance().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showTextNotification(JSONObject data) {
        if (!TextUtils.isEmpty(AppData.activeRoomId) && AppData.activeRoomId.equalsIgnoreCase(data.optString("roomId"))) {
            return;
        }
        String channelId = "chat_notification";
        String channelDescription = "Chat Notification";

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(DemoApp.getInstance(), channelId);

        Intent resultIntent = new Intent(DemoApp.getInstance(), ChatDetailsActivity.class);
        resultIntent.putExtra(ChatDetailsActivity.ROOM_ID, data.optString("roomId"));
        resultIntent.putExtra(ChatDetailsActivity.NAME, data.optJSONObject("sender").optString("name"));
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent piResult = PendingIntent.getActivity(DemoApp.getInstance(),
                (int) Calendar.getInstance().getTimeInMillis(), resultIntent, 0);
        mBuilder = mBuilder.setContentIntent(piResult);
        mBuilder.setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Yuwee Demo Text Message").setWhen(0)
                .setContentTitle("Yuwee Demo Text Message")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(data.optString("message")))
                .setContentText(data.optString("message"))
                .setLights(ContextCompat.getColor(
                        DemoApp.getInstance(), R.color.colorPrimary), 1000, 1000)
                .setDefaults(Notification.DEFAULT_SOUND);


        NotificationManager notificationManager = (NotificationManager) DemoApp.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager, channelId, channelDescription);
        notificationManager.notify(1, mBuilder.build());
    }

    private static void createNotificationChannel(NotificationManager notificationManager, String channelId, String channelDescription) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(channelId, channelDescription, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

    }
}
