package com.yuwee.yuweesdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.adapter.PagerAdapter;
import com.yuwee.yuweesdkdemo.databinding.ActivityChatBinding;
import com.yuwee.yuweesdkdemo.fragment.DirectChatFragment;
import com.yuwee.yuweesdkdemo.fragment.GroupChatFragment;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.model.ViewType;
import com.yuwee.yuweesdkdemo.singleton.ChatImpl;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnFetchChatListListener;
import com.yuwee.sdk.listener.OnFetchChatRoomListener;
import com.yuwee.sdk.model.chat_fetch_list.YuweeChatListModel;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ChatActivity extends AppCompatActivity implements ChatImpl.ChatListener {


    private ActivityChatBinding viewBinding = null;
    private PagerAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_chat);

        setUpToolbar();
        setUpTab();
        setUpViewPager();
        setChatListener();
        getChatList();

    }

    private void setChatListener() {
        ChatImpl.getInstance().setChatListener(this);
    }

    private void getChatList() {
        DialogUtils.showDialog(this, "Loading chat list...");
        Yuwee.getInstance().getChatManager().fetchChatList(new OnFetchChatListListener() {
            @Override
            public void onFetchChatListSuccess(YuweeChatListModel yuWeeChatListModel) {

                if (yuWeeChatListModel.status.equalsIgnoreCase("success")) {
                    getDirectChatFragment().update(yuWeeChatListModel);
                    getGroupChatFragment().update(yuWeeChatListModel);
                }

                DialogUtils.cancelDialog(ChatActivity.this);
            }

            @Override
            public void onFetchChatListFailed(String s) {
                DialogUtils.cancelDialog(ChatActivity.this);
            }
        });
    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setUpTab() {
        viewBinding.tabLayout.addTab(viewBinding.tabLayout.newTab().setText("Start Call"));

        viewBinding.tabLayout.addTab(viewBinding.tabLayout.newTab().setText("Recent Call"));
    }

    private void setUpViewPager() {
        adapter = new PagerAdapter(getSupportFragmentManager(), 1);
        viewBinding.viewPager.setAdapter(adapter);
        viewBinding.tabLayout.setupWithViewPager(viewBinding.viewPager);
    }

    private DirectChatFragment getDirectChatFragment() {
        return (DirectChatFragment) adapter.instantiateItem(viewBinding.viewPager, 0);
    }

    private GroupChatFragment getGroupChatFragment() {
        return (GroupChatFragment) adapter.instantiateItem(viewBinding.viewPager, 1);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chat with");
        LinearLayout linearLayout = new LinearLayout(this);

        final EditText editText = new EditText(this);
        editText.setHint("Enter email separated by commas");


        linearLayout.addView(editText);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 5, 0, 5);
        editText.setLayoutParams(lp);

        builder.setView(linearLayout);
        builder.setPositiveButton("Start", (dialog, which) -> processInput(editText.getText().toString()));

        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });

        builder.show();

    }

    private void processInput(String s) {
        final String[] email = s.split(",");
        ArrayList<String> emailList = new ArrayList<>();
        if (email.length == 1) {
            if (Patterns.EMAIL_ADDRESS.matcher(email[0].trim()).matches()) {
                emailList.add(email[0].trim());
                fetchRoom(emailList);
            } else {
                Utils.showToast("Entered email is not valid.");
            }
        } else {
            for (int i = 0; i < email.length; i++) {
                if (!Patterns.EMAIL_ADDRESS.matcher(email[i].trim()).matches()) {
                    Utils.showToast("No. " + (i + 1) + " email is not valid.");
                    return;
                } else {
                    emailList.add(email[i].trim());
                }
            }
            fetchRoom(emailList);
        }
    }

    private void fetchRoom(final ArrayList<String> emailList) {
        DialogUtils.showDialog(this, "Please wait..");
        Yuwee.getInstance().getChatManager().fetchChatRoomByEmails(emailList, true, new OnFetchChatRoomListener() {
            @Override
            public void onFetchChatRoomSuccess(@NotNull JSONObject jsonObject) {
                DialogUtils.cancelDialog(ChatActivity.this);
                try {
                    String roomId = jsonObject.getJSONObject("result").getJSONObject("room").getString("_id");
                    Intent intent = new Intent(ChatActivity.this, ChatDetailsActivity.class);
                    intent.putExtra(ChatDetailsActivity.ROOM_ID, roomId);
                    if (jsonObject.getJSONObject("result").getJSONObject("room").getBoolean("isGroupChat")) {
                        intent.putExtra(ChatDetailsActivity.NAME, jsonObject.getJSONObject("result").getJSONObject("room").getJSONObject("groupInfo").getString("name"));
                    } else {
                        intent.putExtra(ChatDetailsActivity.NAME, emailList.get(0));
                    }
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFetchChatRoomFailed(String s) {
                DialogUtils.cancelDialog(ChatActivity.this);
            }
        });
    }

    @Override
    public void onNewMessageReceived(JSONObject object) {

        ChatModel chatModel = new ChatModel(Parcel.obtain());

        if (object.optString("messageType").equalsIgnoreCase("CALL")) {
            chatModel.viewType = ViewType.CALL;
            chatModel.callData = new ChatModel.CallData(Parcel.obtain());
            chatModel.callData.callerId = object.optJSONObject("sender").optString("_id");

/*            if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                chatModel.callData.calleeName = name;
                chatModel.callData.callerName = object.optJSONObject("sender").optString("name");
            } else {
                chatModel.callData.calleeName = object.optJSONObject("sender").optString("name");
                chatModel.callData.callerName = name;
            }*/
        } else if (object.optString("messageType").equalsIgnoreCase("FILE")) {
            chatModel.viewType = ViewType.MY_FILE;
        } else {

            chatModel.viewType = ViewType.MY_MESSAGE;
            chatModel.message = object.optString("message");
        }

        chatModel.name = object.optJSONObject("sender").optString("name");
        chatModel.messageId = object.optString("messageId");
        chatModel.roomId = object.optString("roomId");
        chatModel.messageTime = String.valueOf(object.optLong("dateOfCreation"));
        chatModel.lastMessageTime = object.optLong("dateOfCreation");

        if (object.optBoolean("isGroup")) {
            chatModel.name = object.optJSONObject("group").optString("name");
            getGroupChatFragment().updateNow(chatModel, true);
        } else {
            getDirectChatFragment().updateNow(chatModel, true);
        }
    }

    @Override
    public void onUserTypingInRoom(JSONObject jsonObject) {

    }

    @Override
    public void onMessageDeleted(JSONObject jsonObject) {

    }

    @Override
    public void onMessageDeliverySuccess(JSONObject object) {
        ChatModel chatModel = new ChatModel(Parcel.obtain());

        if (object.optString("messageType").equalsIgnoreCase("CALL")) {
            chatModel.viewType = ViewType.CALL;
            chatModel.callData = new ChatModel.CallData(Parcel.obtain());
            chatModel.callData.callerId = object.optJSONObject("sender").optString("_id");

/*            if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                chatModel.callData.calleeName = name;
                chatModel.callData.callerName = object.optJSONObject("sender").optString("name");
            } else {
                chatModel.callData.calleeName = object.optJSONObject("sender").optString("name");
                chatModel.callData.callerName = name;
            }*/
        } else if (object.optString("messageType").equalsIgnoreCase("FILE")) {
            chatModel.viewType = ViewType.MY_FILE;
        } else {

            chatModel.viewType = ViewType.MY_MESSAGE;
            chatModel.message = object.optString("message");
        }

        //chatModel.name = object.optJSONObject("sender").optString("name");
        chatModel.messageId = object.optString("messageId");
        chatModel.roomId = object.optString("roomId");
        chatModel.messageTime = String.valueOf(object.optLong("dateOfCreation"));
        chatModel.lastMessageTime = object.optLong("dateOfCreation");
        //chatModel.name = object.optJSONArray("receivers").optJSONObject(0)

        if (object.optBoolean("isGroup")) {
            chatModel.name = object.optJSONObject("group").optString("name");
            getGroupChatFragment().updateNow(chatModel, false);
        } else {
            chatModel.name = object.optJSONArray("receivers").optJSONObject(0).optString("name");
            getDirectChatFragment().updateNow(chatModel, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatImpl.getInstance().removeChatListener(this);
    }
}
