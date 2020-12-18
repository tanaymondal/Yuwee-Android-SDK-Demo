package com.yuwee.yuweesdkdemo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.yuwee.sdk.model.user_login.UserLoginResponse;
import com.yuwee.yuweesdkdemo.app.DemoApp;

public class PrefUtils {
    private static final PrefUtils instance = new PrefUtils();
    private static SharedPreferences sharedPreferences = null;
    private static SharedPreferences.Editor editor = null;
    private static final String PREF_NAME = "demo_pref";
    private static final String LOGIN_KEY = "login_key";

    @SuppressLint("CommitPrefEdits")
    public synchronized static PrefUtils getInstance() {
        if (sharedPreferences == null) {
            sharedPreferences = DemoApp.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        if (editor == null) {
            editor = sharedPreferences.edit();
        }
        return instance;
    }

    public void saveLogin(UserLoginResponse userLoginResponse) {
        editor.putString(LOGIN_KEY, new Gson().toJson(userLoginResponse)).apply();
    }

    public UserLoginResponse getUserLogin() {
        String data = sharedPreferences.getString(LOGIN_KEY, "");
        return new Gson().fromJson(data, UserLoginResponse.class);
    }
}
