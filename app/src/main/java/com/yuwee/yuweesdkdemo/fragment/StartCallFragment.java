package com.yuwee.yuweesdkdemo.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.MainCallActivity;
import com.yuwee.yuweesdkdemo.databinding.FragmentStartCallBinding;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.enums.MediaType;
import com.yuwee.sdk.listener.OnSetUpCallListener;
import com.yuwee.sdk.model.CallParams;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class StartCallFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {


    private FragmentStartCallBinding viewBinding = null;
    private int selectedItemForCallType = 0;
    private int selectedItemForMediaType = 0;

    public StartCallFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        viewBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_start_call, container, false);

        viewBinding.btnStartCall.setOnClickListener(this);
        setUpSpinner();

        return viewBinding.getRoot();
    }

    private void setUpSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{"DIRECT", "GROUP"});
        viewBinding.spinner.setAdapter(adapter);
        viewBinding.spinner.setOnItemSelectedListener(this);
        viewBinding.spinner.setTag("CALL_TYPE");

        ArrayAdapter<String> mediaAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{"VIDEO", "AUDIO"});
        viewBinding.spinnerMediaType.setAdapter(mediaAdapter);
        viewBinding.spinnerMediaType.setOnItemSelectedListener(this);
        viewBinding.spinnerMediaType.setTag("MEDIA_TYPE");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_call:
                String emails = viewBinding.etEmail.getText().toString().trim();

                if (emails.trim().length() < 1) {
                    viewBinding.tilEmail.setError("Please enter email.");
                    return;
                }

                final String[] email = emails.split(",");

                if (selectedItemForCallType == 0) {
                    if (email.length == 1) {
                        if (Patterns.EMAIL_ADDRESS.matcher(email[0].trim()).matches()) {
                            startDirectCall(email[0].trim());
                        } else {
                            viewBinding.tilEmail.setError("Entered email is not valid.");
                            return;
                        }
                    } else {
                        Utils.showToast("You can not direct call to multiple emails.");
                    }
                } else {
                    if (email.length > 0) {
                        ArrayList<String> emailList = new ArrayList<>();
                        for (int i = 0; i < email.length; i++) {
                            if (!Patterns.EMAIL_ADDRESS.matcher(email[i].trim()).matches()) {
                                viewBinding.tilEmail.setError("No. " + (i + 1) + " email is not valid.");
                                return;
                            } else {
                                emailList.add(email[i].trim());
                            }
                        }

                        startGroupCall(emailList);

                        // TODO: 11/8/18
                    }
                }

                break;
        }
    }

    private void startDirectCall(String email) {
        DialogUtils.showDialog(getActivity(), "Calling...");
        Yuwee.getInstance().getCallManager().setUpCall(getCallParamsForOne2OneCall(email), new OnSetUpCallListener() {
            @Override
            public void onAllUsersOffline() {
                Utils.log("onAllUsersOffline");
                Utils.showToast("All users are offline.");
                DialogUtils.cancelDialog(getActivity());
            }

            @Override
            public void onAllUsersBusy() {
                Utils.log("onAllUsersBusy");
                Utils.showToast("All users are busy.");
                DialogUtils.cancelDialog(getActivity());
            }

            @Override
            public void onReadyToInitiateCall(CallParams callParams, ArrayList<String> busyUserList) {
                Utils.log("onReadyToInitiateCall " + callParams.mediaType.getType());
                DialogUtils.cancelDialog(getActivity());
                startCallActivity(callParams.mediaType.getType());
            }

            @Override
            public void onError(CallParams callParams, String message) {
                Utils.log("onError " + message);
                Utils.showToast(message);
                DialogUtils.cancelDialog(getActivity());
            }
        });
    }

    private void startGroupCall(ArrayList<String> emails) {
        DialogUtils.showDialog(getActivity(), "Calling...");
        Yuwee.getInstance().getCallManager().setUpCall(getCallParamsForGroupCall(emails), new OnSetUpCallListener() {
            @Override
            public void onAllUsersOffline() {
                Utils.log("onAllUsersOffline");
                Utils.showToast("All users are offline.");
                DialogUtils.cancelDialog(getActivity());
            }

            @Override
            public void onAllUsersBusy() {
                Utils.log("onAllUsersBusy");
                Utils.showToast("All users are busy.");
                DialogUtils.cancelDialog(getActivity());
            }

            @Override
            public void onReadyToInitiateCall(CallParams callParams, ArrayList<String> arrayList) {
                Utils.log("onReadyToInitiateCall " + callParams.mediaType.getType());
                DialogUtils.cancelDialog(getActivity());
                startCallActivity(callParams.mediaType.getType());
            }

            @Override
            public void onError(CallParams callParams, String s) {
                Utils.log("onError");
                Utils.showToast(s);
                DialogUtils.cancelDialog(getActivity());
            }
        });
    }

    private void startCallActivity(String type) {
        Intent intent = new Intent(getContext(), MainCallActivity.class);
        intent.putExtra(MainCallActivity.MEDIA_TYPE, type);
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getTag().toString().equalsIgnoreCase("MEDIA_TYPE")) {
            selectedItemForMediaType = position;
        } else {
            selectedItemForCallType = position;
            viewBinding.etEmail.setText("");
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private CallParams getCallParamsForOne2OneCall(String email) {
        CallParams callParams = new CallParams();
        callParams.mediaType = selectedItemForMediaType == 0 ? MediaType.VIDEO : MediaType.AUDIO;
        callParams.invitationMessage = "Hi, lets have a call..";
        callParams.inviteeEmail = email;
        callParams.inviteeName = "Test User";
        callParams.isGroup = false;
        return callParams;
    }

    private CallParams getCallParamsForGroupCall(ArrayList<String> emails) {
        CallParams callParams = new CallParams();
        callParams.mediaType = selectedItemForMediaType == 0 ? MediaType.VIDEO : MediaType.AUDIO;
        callParams.isGroup = true;
        callParams.groupName = "123 Test Group";
        callParams.invitationMessage = "Hi, lets have a call..";
        callParams.groupEmailList = emails;
        return callParams;
    }
}
