package com.yuwee.yuweesdkdemo.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityScheduleBinding;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnMeetingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ScheduleActivity extends AppCompatActivity {

    private static final int REQ_CODE = 101;
    private ActivityScheduleBinding viewBinding = null;
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_schedule);

        setUpToolbar();
    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Schedule Meeting");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewBinding.rdGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rd_room_id) {
                viewBinding.tilScheduleEmails.setVisibility(View.GONE);
                viewBinding.btnRoom.setVisibility(View.VISIBLE);
                viewBinding.tvRoomDetails.setText("");
                viewBinding.tvRoomDetails.setVisibility(View.VISIBLE);
            } else {
                viewBinding.tilScheduleEmails.setVisibility(View.VISIBLE);
                viewBinding.btnRoom.setVisibility(View.GONE);
                viewBinding.tvRoomDetails.setVisibility(View.GONE);
                roomId = null;
            }
        });
    }

    public void schedule(View view) {

        if (viewBinding.etScheduleName.getText().toString().trim().isEmpty()) {
            viewBinding.tilScheduleName.setError("Please enter a schedule name");
            return;
        }

        if (viewBinding.etScheduleName.getText().toString().trim().length() < 3) {
            viewBinding.tilScheduleName.setError("Schedule name must be at least of 3 characters.");
            return;
        }

        if (viewBinding.tvDate.getText().toString().equalsIgnoreCase("SELECT")) {
            Utils.showToast("Please select date");
            return;
        }

        if (viewBinding.tvTime.getText().toString().equalsIgnoreCase("SELECT")) {
            Utils.showToast("Please select time");
            return;
        }


        Date selectedDate = null;
        try {
            selectedDate = simpleDateFormat.parse(viewBinding.tvDate.getText().toString().trim() + " " +
                    viewBinding.tvTime.getText().toString().trim());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        assert selectedDate != null;
        if (((selectedDate.getTime() - new Date().getTime()) / (1000 * 60)) < 10) {
            Utils.showToast("Meeting time should be 10 minutes more than current time.");
            return;
        }

        JSONArray jsonArray = new JSONArray();
        if (viewBinding.rdRoomId.isChecked()) {
            if (TextUtils.isEmpty(roomId)) {
                Utils.showToast("Please select room");
                return;
            }
        } else {
            final String[] email = viewBinding.etScheduleEmails.getText().toString().split(",");


            if (email.length > 1) {

                for (int i = 0; i < email.length; i++) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email[i].trim()).matches()) {
                        Utils.showToast("No. " + (i + 1) + " email is not valid.");
                        return;
                    } else {
                        jsonArray.put(email[i].trim());
                    }
                }
            } else {
                if (!Patterns.EMAIL_ADDRESS.matcher(viewBinding.etScheduleEmails.getText().toString().trim()).matches()) {
                    viewBinding.tilScheduleEmails.setError("Please add at least 1 valid email.");
                    return;
                } else {
                    jsonArray.put(viewBinding.etScheduleEmails.getText().toString().trim());
                }
            }
            if (jsonArray.length() == 0) {
                viewBinding.tilScheduleEmails.setError("Please add at least 1 valid email.");
                return;
            }
        }

        JSONObject object = new JSONObject();
        try {
            object.put("schedulerName", viewBinding.etScheduleName.getText().toString().trim());
            object.put("schedulerDescription", viewBinding.etDesc.getText().toString().trim());
            object.put("dateOfStart", viewBinding.tvDate.getText().toString().trim());
            object.put("time", viewBinding.tvTime.getText().toString().trim());
            object.put("duration", "00:10");
            object.put("schedulerMedia", "VIDEO");
            object.put("timezone", "GMT+05:30");
            object.put("alertBeforeMeeting", "5");
            if (viewBinding.rdRoomId.isChecked()) {
                object.put("roomId", roomId);
            } else {
                object.put("members", jsonArray.toString());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        DialogUtils.showDialog(this, "Creating meeting...");
        Yuwee.getInstance().getCallManager().scheduleMeeting(object, new OnMeetingListener() {
            @Override
            public void onMeetingCreatedSuccessfully(JSONObject response) {
                DialogUtils.cancelDialog(ScheduleActivity.this);
                Utils.showToast("Meeting created successfully.");
                onBackPressed();
            }

            @Override
            public void onMeetingCreationFailure(String error) {
                DialogUtils.cancelDialog(ScheduleActivity.this);
            }
        });


    }

    public void onSelectDate(View view) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                viewBinding.tvDate.setText((month + 1) + "/" + dayOfMonth + "/" + year);
            }
        }, Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DATE));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    public void onSelectTime(View view) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                viewBinding.tvTime.setText(hourOfDay + ":" + minute);
            }
        }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE), false);
        timePickerDialog.show();
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

    public void selectRoom(View view) {
        Intent intent = new Intent(this, SelectRoomActivity.class);
        startActivityForResult(intent, REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE && resultCode == CommonStatusCodes.SUCCESS && data != null) {
            String name = data.getStringExtra("name");
            roomId = data.getStringExtra("roomId");
            viewBinding.tvRoomDetails.setText(name + "\n" + roomId);
        }
    }
}
