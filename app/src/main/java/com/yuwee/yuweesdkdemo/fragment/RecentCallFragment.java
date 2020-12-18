package com.yuwee.yuweesdkdemo.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.SelectRoomActivity;
import com.yuwee.yuweesdkdemo.adapter.RecentCallAdapter;
import com.yuwee.yuweesdkdemo.databinding.FragmentRecentCallBinding;
import com.yuwee.yuweesdkdemo.model.RecentCallModel;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnGetRecentCallListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecentCallFragment extends Fragment implements RecentCallAdapter.NextPageLoadListener {


    private FragmentRecentCallBinding viewBinding = null;
    private RecentCallAdapter adapter = null;
    private ArrayList<RecentCallModel> arrayList = new ArrayList<>();
    private long skip = 0, totalCount;
    private boolean isLoaded = false;
    public RecentCallFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        viewBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recent_call, container, false);

        setUpRecyclerView();

        return viewBinding.getRoot();
    }

    private void getRecentCall() {
        DialogUtils.showDialog(getActivity(), "Loading recent call...");
        Yuwee.getInstance().getCallManager().getRecentCall(String.valueOf(skip), new OnGetRecentCallListener() {
            @Override
            public void onGetRecentCallSuccess(JSONObject jsonObject) {
                parseData(jsonObject);
                DialogUtils.cancelDialog(getActivity());
            }

            @Override
            public void onGetRecentCallError(String s) {
                DialogUtils.cancelDialog(getActivity());
                Utils.showToast("Unable to get recent call data.");
            }
        });
    }

    private void parseData(JSONObject jsonObject) {
        if (jsonObject.optString("status").equalsIgnoreCase("success")) {
            JSONObject result = jsonObject.optJSONObject("result");
            totalCount = result.optLong("totalRecord");
            JSONArray array = result.optJSONArray("calls");

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                RecentCallModel recentCallModel = new RecentCallModel();
                recentCallModel.callId = object.optString("callId");
                recentCallModel.roomId = object.optString("roomId");
                recentCallModel.confName = object.optString("confName");
                recentCallModel.callTime = String.valueOf(object.optLong("startTime"));
                recentCallModel.isVideoCall = object.optString("callType").equalsIgnoreCase("VIDEO");
                recentCallModel.isVideoCall = object.optBoolean("isOneToOneCall");
                arrayList.add(recentCallModel);
            }

            isLoaded = true;
            adapter.notifyDataSetChanged();
        }
    }

    private void setUpRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        viewBinding.recyclerView.setLayoutManager(manager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), manager.getOrientation());
        viewBinding.recyclerView.addItemDecoration(dividerItemDecoration);

        adapter = new RecentCallAdapter(arrayList);
        adapter.setNextPageLoadListener(this);
        viewBinding.recyclerView.setAdapter(adapter);

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !isLoaded) {
            getRecentCall();
        }
    }


    @Override
    public void loadNextPageNow() {
        if (totalCount > arrayList.size()) {
            skip = skip + 20;
            getRecentCall();
        }
    }
}
