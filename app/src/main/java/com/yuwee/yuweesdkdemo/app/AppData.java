package com.yuwee.yuweesdkdemo.app;

import android.content.res.Resources;

public class AppData {
    public static String activeRoomId = null;
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
