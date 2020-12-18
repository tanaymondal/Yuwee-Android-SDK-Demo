package com.yuwee.yuweesdkdemo.adapter;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.yuwee.yuweesdkdemo.fragment.DirectChatFragment;
import com.yuwee.yuweesdkdemo.fragment.GroupChatFragment;
import com.yuwee.yuweesdkdemo.fragment.RecentCallFragment;
import com.yuwee.yuweesdkdemo.fragment.ScheduleFragment;
import com.yuwee.yuweesdkdemo.fragment.StartCallFragment;

import org.jetbrains.annotations.NotNull;


public class PagerAdapter extends FragmentPagerAdapter {
    private int type; // 0 = call, 1 = chat

    public PagerAdapter(FragmentManager fm, int type) {
        super(fm);
        this.type = type;
    }

    @NotNull
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                return type == 0 ? new StartCallFragment() : new DirectChatFragment();
            case 1:
                return type == 0 ? new RecentCallFragment() : new GroupChatFragment();
            case 2:
                return new ScheduleFragment();
        }
        return new Fragment();
    }

    @Override
    public int getCount() {
        return type == 0 ? 3 : 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return type == 0 ? "START CALL" : "DIRECT CHAT";
            case 1:
                return type == 0 ? "RECENT CALL" : "GROUP CHAT";
            case 2:
                return "SCHEDULE CALL";

        }
        return "";
    }
}
