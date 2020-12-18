package com.yuwee.yuweesdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.singleton.CallImpl;
import com.yuwee.yuweesdkdemo.utils.PrefUtils;
import com.yuwee.sdk.Yuwee;

import retrofit2.http.Url;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(PrefUtils.getInstance().getUserLogin().result.user.email);
        }

        CallImpl.getInstance().initListener();
    }

    public void onCallClicked(View view) {
        startActivity(new Intent(this, CallActivity.class));
    }

    public void onChatClicked(View view) {
        startActivity(new Intent(this, ChatActivity.class));
    }

    public void onLogoutClicked(View view) {
        Yuwee.getInstance().getUserManager().logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void openContactActivity(View view) {
        startActivity(new Intent(this, ContactActivity.class));
    }

    public void openMeetingActivity(View view) {
        startActivity(new Intent(this, MeetingActivity.class));
    }
}
