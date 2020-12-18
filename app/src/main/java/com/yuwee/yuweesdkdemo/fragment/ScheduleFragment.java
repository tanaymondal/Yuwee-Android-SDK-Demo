package com.yuwee.yuweesdkdemo.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.MainCallActivity;
import com.yuwee.yuweesdkdemo.adapter.ScheduleListAdapter;
import com.yuwee.yuweesdkdemo.model.ScheduleModel;
import com.yuwee.yuweesdkdemo.singleton.ScheduleImpl;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.enums.MediaType;
import com.yuwee.sdk.listener.OnGetScheduleMeetingListener;
import com.yuwee.sdk.listener.OnJoinOngoingCallListener;
import com.yuwee.sdk.listener.OnJoinScheduleCallListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment implements ScheduleListAdapter.OnListItemClickListener, ScheduleImpl.OnScheduleListener, View.OnClickListener {


    private RecyclerView recyclerView;
    private ScheduleListAdapter adapter = null;
    private ArrayList<ScheduleModel> arrayList = new ArrayList<>();
    private boolean isLoaded = false;
    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);

        ScheduleImpl.getInstance().setScheduleListener(this);

        setUpRecyclerView();

        view.findViewById(R.id.btn_join).setOnClickListener(this);

        return view;
    }

    private void getScheduleCallList() {
        DialogUtils.showDialog(getContext(), "Loading upcoming calls..");
        //Toast.makeText(DemoApp.getInstance(), "Loading upcoming calls..", Toast.LENGTH_SHORT).show();
        Yuwee.getInstance().getCallManager().getAllUpcomingCalls(new OnGetScheduleMeetingListener() {
            @Override
            public void onGetUpcomingCallSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(getActivity());
                Utils.log("onGetUpcomingCallSuccess " + jsonObject);
                isLoaded = true;


                JSONArray result = jsonObject.optJSONArray("result");
                for (int i = 0; i < result.length(); i++) {
                    JSONObject object = result.optJSONObject(i);

                    ScheduleModel scheduleModel = new ScheduleModel();
                    scheduleModel.creatorId = object.optString("schedulerInitiator");
                    scheduleModel.groupId = object.optString("groupId");
                    scheduleModel.groupName = object.optString("schedulerName");
                    scheduleModel.meetingDate = String.valueOf(object.optLong("meetingDate"));
                    scheduleModel.scheduleId = object.optString("schedulerId");
                    scheduleModel.callId = object.optString("callId", "");

                    arrayList.add(scheduleModel);

                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onGetUpcomingCallError(String s) {
                DialogUtils.cancelDialog(getActivity());
                Utils.log("onGetUpcomingCallError " + s);
            }
        });
    }

    private void setUpRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(manager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), manager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        adapter = new ScheduleListAdapter(arrayList);
        adapter.setOnListItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onListItemClicked(int position) {
        Yuwee.getInstance().getCallManager().joinMeeting(arrayList.get(position).callId, MediaType.VIDEO, new OnJoinScheduleCallListener() {
            @Override
            public void onJoinSuccess() {
                Utils.log("onJoinSuccess");
                startActivity(new Intent(getActivity(), MainCallActivity.class));
            }

            @Override
            public void onError() {
                Utils.log("onError");
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !isLoaded) {
            getScheduleCallList();
        }
    }

    @Override
    public void onNewScheduledCall(JSONObject jsonObject) {
        Utils.log("onNewScheduledCall " + jsonObject);
        ScheduleModel scheduleModel = new ScheduleModel();
        scheduleModel.creatorId = jsonObject.optString("creatorId");
        scheduleModel.userId = jsonObject.optString("userId");
        scheduleModel.groupId = jsonObject.optString("groupId");
        scheduleModel.groupName = jsonObject.optString("groupName");
        scheduleModel.meetingDate = String.valueOf(jsonObject.optJSONArray("meetingDates").optJSONObject(0).optLong("meetingDate"));
        scheduleModel.scheduleId = jsonObject.optString("schedulerId");

        arrayList.add(scheduleModel);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onScheduledCallActivated(JSONObject jsonObject) {
        Utils.log("onScheduledCallActivated " + jsonObject);
        String schedulerId = jsonObject.optString("schedulerId");

        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).scheduleId.equalsIgnoreCase(schedulerId)) {
                arrayList.get(i).callId = jsonObject.optString("callId");
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onScheduledCallDeleted(JSONObject jsonObject) {
        Utils.log("onScheduledCallDeleted " + jsonObject);
    }

    @Override
    public void onScheduleCallExpired(JSONObject jsonObject) {
        Utils.log("onScheduleCallExpired " + jsonObject);
        String schedulerId = jsonObject.optString("schedulerId");

        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).scheduleId.equalsIgnoreCase(schedulerId)) {
                arrayList.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        ScheduleImpl.getInstance().removeScheduleListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_join:

                joinMeeting();

                break;
        }
    }

    private void joinMeeting() {
        Yuwee.getInstance().getCallManager().joinOngoingCall("61675337-35b1-4260-8f8b-77726389e5d0", MediaType.VIDEO, new OnJoinOngoingCallListener() {
            @Override
            public void onJoinOngoingCallSuccess() {
                Utils.log("onJoinOngoingCallSuccess");
                startActivity(new Intent(getActivity(), MainCallActivity.class));
            }

            @Override
            public void onError(String s) {
                Utils.log("onError");
            }
        });
    }
}
