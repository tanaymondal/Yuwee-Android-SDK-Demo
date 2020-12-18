package com.yuwee.yuweesdkdemo.activity;

import android.content.Intent;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;

import androidx.databinding.DataBindingUtil;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.adapter.PagerAdapter;
import com.yuwee.yuweesdkdemo.databinding.ActivityCallBinding;
import com.yuwee.yuweesdkdemo.utils.Utils;

public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding viewBinding = null;
    private PagerAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_call);
        setUpToolbar();
        setUpTab();
        setUpViewPager();
    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Call");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setUpTab() {
        viewBinding.tabLayout.addTab(viewBinding.tabLayout.newTab().setText("Start Call"));

        viewBinding.tabLayout.addTab(viewBinding.tabLayout.newTab().setText("Recent Call"));
        viewBinding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (getCurrentFocus() != null) {
                    Utils.hideKeyboard(getCurrentFocus());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void setUpViewPager() {
        viewBinding.fab.hide();
        adapter = new PagerAdapter(getSupportFragmentManager(), 0);
        viewBinding.viewPager.setAdapter(adapter);
        viewBinding.viewPager.setOffscreenPageLimit(3);
        viewBinding.tabLayout.setupWithViewPager(viewBinding.viewPager);
        viewBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    viewBinding.fab.show();
                } else {
                    viewBinding.fab.hide();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    public void onFabClicked(View view) {
        startActivity(new Intent(this, ScheduleActivity.class));
    }
}
