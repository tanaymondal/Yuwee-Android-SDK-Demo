package com.yuwee.yuweesdkdemo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.adapter.ChatDetailsAdapter;
import com.yuwee.yuweesdkdemo.app.AppData;
import com.yuwee.yuweesdkdemo.databinding.ActivityChatDetailsBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutAddMemberBinding;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.model.ViewType;
import com.yuwee.yuweesdkdemo.singleton.ChatImpl;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.PrefUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.pickit.PickiT;
import com.yuwee.pickit.PickiTCallbacks;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.enums.MessageDeleteType;
import com.yuwee.sdk.listener.InitFileShareListener;
import com.yuwee.sdk.listener.OnAddMembersInGroupByEmailListener;
import com.yuwee.sdk.listener.OnDeleteMessageInRoomListener;
import com.yuwee.sdk.listener.OnFetchChatMessageListener;
import com.yuwee.sdk.listener.OnGetMessageByRangeListener;
import com.yuwee.sdk.listener.OnRemoveMembersFromGroupByEmailListener;
import com.yuwee.sdk.listener.SendFileListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

public class ChatDetailsActivity extends AppCompatActivity implements ChatDetailsAdapter.OnChatInteractListener, ChatImpl.ChatListener, PickiTCallbacks {

    public static final String ROOM_ID = "room_id";
    public static final String NAME = "name";
    public static final String IS_GROUP_CHAT = "is_group_chat";
    private String roomId, name;
    private int skip = 0;
    private ArrayList<ChatModel> arrayList = new ArrayList<>();
    private ActivityChatDetailsBinding viewBinding = null;
    private ChatDetailsAdapter adapter;
    private int totalCount = 0;
    private PickiT pickiT;
    private Handler handler;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("");
                handler = null;
            }
        }
    };
    private boolean isGroupChat = false;
    private boolean isQuoteOn = false;
    private ChatModel quoteChatModel = null;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_chat_details);

        pickiT = new PickiT(this, this);
        getIntentData();
        setUpToolbar();
        setChatListener();
        setUpRecyclerView();
        getRoomMessages();
        typingListener();
        Yuwee.getInstance().getChatManager().getFileManager().initFileShare(roomId, new InitFileShareListener() {
            @Override
            public void onInitFileShareSuccess() {
                Utils.log("onInitFileShareSuccess");
            }

            @Override
            public void onInitFileShareFailed() {
                Utils.log("onInitFileShareFailed");
            }
        });

        Yuwee.getInstance().getChatManager().markMessagesReadInRoom(roomId);
        ChatImpl.getInstance().updateCount(roomId);
    }

    private void typingListener() {
        viewBinding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Yuwee.getInstance().getChatManager().sendTypingStatus(roomId);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setChatListener() {
        ChatImpl.getInstance().setChatListener(this);
    }

    private void setUpRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);

        viewBinding.recyclerView.setLayoutManager(manager);
        adapter = new ChatDetailsAdapter(arrayList, this);
        adapter.setOnItemInteractListener(this);
        viewBinding.recyclerView.setAdapter(adapter);
    }

    private void getIntentData() {
        name = getIntent().getExtras().getString(NAME);
        roomId = getIntent().getExtras().getString(ROOM_ID);
        isGroupChat = getIntent().getExtras().getBoolean(IS_GROUP_CHAT);
        AppData.activeRoomId = roomId;
    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setSubtitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void getRoomMessages() {
        DialogUtils.showDialog(this, "Loading messages...");
        Yuwee.getInstance().getChatManager().fetchChatMessages(roomId, String.valueOf(skip), new OnFetchChatMessageListener() {
            @Override
            public void onFetchChatMessageSuccess(JSONObject jsonObject) {
                parseChatData(jsonObject, false);
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
            }

            @Override
            public void onFetchChatMessageFailed(String s) {
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
            }
        });
    }

    private void parseChatData(JSONObject jsonObject, boolean isFromQuote) {
        JSONObject result = jsonObject.optJSONObject("result");

        JSONArray message = result.optJSONObject("result").optJSONArray("messages");

        if (isFromQuote) {
            skip = skip + message.length();
        }

        if (!isFromQuote && message.length() < 20) {
            isLastPage = true;
        }


        for (int i = message.length() - 1; i >= 0; i--) {
            JSONObject object = message.optJSONObject(i);
            ChatModel chatModel = new ChatModel(Parcel.obtain());
            chatModel.messageId = object.optString("messageId");
            chatModel.messageTime = String.valueOf(object.optLong("dateOfCreation"));
            chatModel.senderId = object.optJSONObject("sender").optString("_id");
            chatModel.senderName = object.optJSONObject("sender").optString("name");
            chatModel.senderImage = object.optJSONObject("sender").optString("image");
            chatModel.senderEmail = object.optJSONObject("sender").optString("email");
            chatModel.messageType = object.optString("messageType");
            chatModel.quoteJson = object.optJSONObject("quotedMessage") == null ? null : object.optJSONObject("quotedMessage").toString();

            if (object.optString("messageType").equalsIgnoreCase("CALL")) {
                chatModel.viewType = ViewType.CALL;
                chatModel.callData = new ChatModel.CallData(Parcel.obtain());
                chatModel.callData.callerId = object.optJSONObject("sender").optString("_id");

                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.callData.calleeName = name;
                    chatModel.callData.callerName = object.optJSONObject("sender").optString("name");
                } else {
                    chatModel.callData.calleeName = object.optJSONObject("sender").optString("name");
                    chatModel.callData.callerName = name;
                }
            } else if (object.optString("messageType").equalsIgnoreCase("FILE")) {
                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_FILE;
                } else {
                    chatModel.viewType = ViewType.OTHER_FILE;
                }

                chatModel.fileData = new ChatModel.FileData(Parcel.obtain());
                chatModel.fileData.fileId = object.optJSONObject("fileInfo").optString("_id");
                chatModel.fileData.fileKey = object.optJSONObject("fileInfo").optString("fileKey");
                //chatModel.fileData.fileUrl = object.optJSONObject("fileInfo").optString("downloadUrl");
                chatModel.fileData.fileName = object.optJSONObject("fileInfo").optString("fileName");

            } else {
                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_MESSAGE;
                } else {
                    chatModel.viewType = ViewType.OTHERS_MESSAGE;
                }
                chatModel.message = object.optString("message");
            }

            chatModel.name = object.optJSONObject("sender").optString("name");
            chatModel.isForwarded = object.optBoolean("isForwarded");
            arrayList.add(chatModel);


        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_option, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isGroupChat) {
            menu.getItem(0).setVisible(false);
        }
        return true;
    }

    private void addMember() {


        LayoutAddMemberBinding viewBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_add_member, null, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add member");
        builder.setView(viewBinding.getRoot());
        viewBinding.etGroupName.setVisibility(View.GONE);
        builder.setPositiveButton("Add", (dialog, which) -> {
            if (TextUtils.isEmpty(viewBinding.etEmail.getText().toString().trim()) ||
                    !Patterns.EMAIL_ADDRESS.matcher(viewBinding.etEmail.getText().toString().trim()).matches()) {
                Utils.showToast("Please enter valid email.");
                return;
            }
            addMember(viewBinding.etEmail.getText().toString().trim());

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });
        builder.show();
    }

    private void addMember(String email) {
        DialogUtils.showDialog(this, "Adding member...");
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(email);
        Yuwee.getInstance().getChatManager().addMembersInGroupByEmail(roomId, arrayList, new OnAddMembersInGroupByEmailListener() {
            @Override
            public void onAddMemberInGroupSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
                Utils.log(jsonObject);
                Utils.showToast("Member successfully added.");
            }

            @Override
            public void onAddMemberInGroupError(String s) {
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
                Utils.log(s);
                Utils.showToast(s);
            }
        });
    }

    private void removeMember() {
        DialogUtils.showDialog(this, "Removing member...");
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("tanay@yuwee.com"); // hi@xcodes.net
        Yuwee.getInstance().getChatManager().removeMembersFromGroupByEmail(roomId, arrayList, new OnRemoveMembersFromGroupByEmailListener() {

            @Override
            public void onRemoveMemberFromGroupSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
                Utils.log(jsonObject);
            }

            @Override
            public void onRemoveMemberFromGroupError(String s) {
                DialogUtils.cancelDialog(ChatDetailsActivity.this);
                Utils.log(s);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            case R.id.menu_add_member:

                addMember();
                //removeMember();

                break;
        }
        return true;
    }

    public void sendMessage(View view) {
        if (viewBinding.etMessage.getText().toString().trim().length() > 0) {
            long time = Calendar.getInstance().getTimeInMillis();
            String quoteData = null;
            if (isQuoteOn) {

                try {
                    JSONObject object = new JSONObject();
                    object.put("messageType", quoteChatModel.messageType); // text or file
                    object.put("content", quoteChatModel.message);
                    object.put("time", quoteChatModel.messageTime);
                    object.put("_id", quoteChatModel.messageId);
                    if (quoteChatModel.messageType.equalsIgnoreCase("file")) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("fileName", quoteChatModel.fileData.fileName);
                        fileInfo.put("fileId", quoteChatModel.fileData.fileId);
                        //object.put("fileSize", quoteChatModel.fileData.fi);
                        //object.put("fileExt", quoteChatModel.fileData);
                        object.put("downloadUrl", quoteChatModel.fileData.fileUrl);
                        object.put("fileInfo", fileInfo);
                    }


                    JSONObject info = new JSONObject();
                    info.put("_id", quoteChatModel.senderId);
                    info.put("status", "");
                    info.put("email", quoteChatModel.senderEmail);
                    info.put("name", quoteChatModel.senderName);
                    info.put("image", quoteChatModel.senderImage);

                    object.put("senderInfo", info);

                    quoteData = object.toString();

                    Yuwee.getInstance().getChatManager().sendMessage(viewBinding.etMessage.getText().toString().trim(),
                            roomId,
                            String.valueOf(time), quoteChatModel.messageId);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                viewBinding.llQuote.setVisibility(View.GONE);
                isQuoteOn = false;

            } else {
                Yuwee.getInstance().getChatManager().sendMessage(viewBinding.etMessage.getText().toString().trim(),
                        roomId,
                        String.valueOf(time), null);
            }


            ChatModel chatModel = new ChatModel(Parcel.obtain());
            chatModel.viewType = ViewType.MY_MESSAGE;
            chatModel.quoteJson = quoteData;
            chatModel.messageType = "text";
            chatModel.message = viewBinding.etMessage.getText().toString().trim();
            chatModel.messageIdentifier = String.valueOf(time);
            viewBinding.etMessage.setText("");
            arrayList.add(0, chatModel);
            adapter.notifyItemInserted(0);
            viewBinding.recyclerView.scrollToPosition(0);
        } else {
            Utils.showToast("Please enter message to send.");
        }
    }

    // Image permission dialog
    private void uploadImagePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1001);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1001:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    showFileChooser();
                } else {
                    Toast.makeText(ChatDetailsActivity.this, "Please allow all permission to upload files.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // open file chooser
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    999);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {


            case 999:

                if (resultCode == RESULT_OK) {
                    //path = FileUtils.getPath(ChatActivity.this, data.getData());
                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void sendFile(View view) {

        uploadImagePermission();

        /*        { “fileName” : “newFile.png”, “fileExtension” : “png”, “fileSize” : 1024, “downloadUrl” : “www.myImage.com” }*/

       /* String fileName = "google.png";
        String fileUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png";
        JSONObject quoteData = null;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileName", fileName);
            jsonObject.put("fileExtension", "png");
            jsonObject.put("fileSize", "200");
            jsonObject.put("downloadUrl", fileUrl);


            if (isQuoteOn) {


                JSONObject object = new JSONObject();
                object.put("messageType", quoteChatModel.messageType); // text or file
                object.put("content", quoteChatModel.message);
                object.put("time", quoteChatModel.messageTime);
                object.put("_id", quoteChatModel.messageId);
                if (quoteChatModel.messageType.equalsIgnoreCase("file")) {
                    JSONObject fileInfo = new JSONObject();
                    fileInfo.put("fileName", quoteChatModel.fileData.fileName);
                    fileInfo.put("fileId", quoteChatModel.fileData.fileId);
                    //object.put("fileSize", quoteChatModel.fileData.fi);
                    //object.put("fileExt", quoteChatModel.fileData);
                    object.put("downloadUrl", quoteChatModel.fileData.fileUrl);

                    object.put("fileInfo", fileInfo);
                }


                JSONObject info = new JSONObject();
                info.put("_id", quoteChatModel.senderId);
                info.put("status", "");
                info.put("email", quoteChatModel.senderEmail);
                info.put("name", quoteChatModel.senderName);
                info.put("image", quoteChatModel.senderImage);

                object.put("senderInfo", info);

                quoteData = object;


                viewBinding.llQuote.setVisibility(View.GONE);
                //isQuoteOn = false;

                //jsonObject.put("quotedMessage", object);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        long time = Calendar.getInstance().getTimeInMillis();
        Yuwee.getInstance().getChatManager().shareFile(roomId, String.valueOf(time), jsonObject, isQuoteOn ? quoteChatModel.messageId : null);
        isQuoteOn = false;

        ChatModel chatModel = new ChatModel(Parcel.obtain());
        chatModel.viewType = ViewType.MY_FILE;
        chatModel.messageType = "file";
        chatModel.quoteJson = quoteData != null ? quoteData.toString() : null;
        ChatModel.FileData fileData = new ChatModel.FileData(Parcel.obtain());
        fileData.fileName = fileName;
        fileData.fileUrl = fileUrl;

        chatModel.fileData = fileData;
        chatModel.messageIdentifier = String.valueOf(time);
        arrayList.add(0, chatModel);
        adapter.notifyItemInserted(0);
        viewBinding.recyclerView.scrollToPosition(0);*/

    }

    @Override
    public void onItemClickListener(int position) {
        if (!TextUtils.isEmpty(arrayList.get(position).quoteJson)) {
            int pos = -1;
            String messageId = null;
            try {
                messageId = new JSONObject(arrayList.get(position).quoteJson).getString("_id");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (TextUtils.isEmpty(messageId)) {
                Utils.showToast("Unable to find message");
                return;
            }

            for (int i = 0; i < arrayList.size(); i++) {
                if (arrayList.get(i).messageId.equalsIgnoreCase(messageId)) {
                    pos = i;
                    break;
                }
            }


            if (pos == -1) {
                getMessageByRange(position);
            } else {
                viewBinding.recyclerView.smoothScrollToPosition(pos);
            }

        }
    }

    private void getMessageByRange(int position) {
        try {
            JSONObject jsonObject = new JSONObject(arrayList.get(position).quoteJson);
            String firstMessageId = jsonObject.optString("messageId");
            String lastMessageId = arrayList.get(arrayList.size() - 1).messageId;

            Yuwee.getInstance().getChatManager().getMessageByRange(roomId, firstMessageId, lastMessageId, new OnGetMessageByRangeListener() {
                @Override
                public void onGetMessageByRangeSuccess(JSONObject jsonObject) {
                    try {
                        JSONArray messages = jsonObject.optJSONObject("result").optJSONObject("result").getJSONArray("messages");
                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                        for (int i = 0; i < messages.length(); i++) {
                            arrayList.add(messages.optJSONObject(i));
                        }
                        Collections.reverse(arrayList);
                        for (int i = 0; i < arrayList.size(); i++) {
                            messages.put(i, arrayList.get(i));
                        }
                        parseChatData(jsonObject, true);

                        int pos = -1;
                        for (int i = 0; i < ChatDetailsActivity.this.arrayList.size(); i++) {
                            if (ChatDetailsActivity.this.arrayList.get(i).messageId.equalsIgnoreCase(firstMessageId)) {
                                pos = i;
                                break;
                            }
                        }

                        if (pos != -1) {
                            viewBinding.recyclerView.smoothScrollToPosition(pos);
                        } else {
                            Utils.showToast("Unable to find message");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onGetMessageByRangeError(String error) {
                    Utils.showToast("Unable to find message");
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemLongClickListener(int position) {
        showDeleteDialog(position);
    }

    @Override
    public void onLoadNextPage() {
        if (!isLastPage) {
            skip = skip + 20;
            getRoomMessages();
        }
    }

    @Override
    public void onNewMessageReceived(JSONObject object) {
        Utils.log("onNewMessageReceived " + object);
        if (roomId.equalsIgnoreCase(object.optString("roomId"))) {
            ChatModel chatModel = new ChatModel(Parcel.obtain());

            if (object.optString("messageType").equalsIgnoreCase("CALL")) {
                chatModel.viewType = ViewType.CALL;
                chatModel.callData = new ChatModel.CallData(Parcel.obtain());
                chatModel.callData.callerId = object.optJSONObject("sender").optString("_id");
                //chatModel.messageType = object.optString("messageType");

                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.callData.calleeName = name;
                    chatModel.callData.callerName = object.optJSONObject("sender").optString("name");
                } else {
                    chatModel.callData.calleeName = object.optJSONObject("sender").optString("name");
                    chatModel.callData.callerName = name;
                }
            } else if (object.optString("messageType").equalsIgnoreCase("FILE")) {
                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_FILE;
                } else {
                    chatModel.viewType = ViewType.OTHER_FILE;
                }

                chatModel.fileData = new ChatModel.FileData(Parcel.obtain());
                chatModel.fileData.fileId = object.optJSONObject("fileInfo").optString("_id");
                chatModel.fileData.fileKey = object.optJSONObject("fileInfo").optString("fileKey");
                chatModel.fileData.fileName = object.optJSONObject("fileInfo").optString("fileName");
                //chatModel.messageType = "file";

            } else {
                if (object.optJSONObject("sender").optString("_id").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_MESSAGE;
                } else {
                    chatModel.viewType = ViewType.OTHERS_MESSAGE;
                }
                chatModel.message = object.optString("message");
                //chatModel.messageType = "text";
            }

            chatModel.messageType = object.optString("messageType");
            chatModel.name = object.optJSONObject("sender").optString("name");
            chatModel.messageId = object.optString("messageId");
            chatModel.messageTime = String.valueOf(object.optLong("dateOfCreation"));
            chatModel.isForwarded = object.optBoolean("isForwarded");
            chatModel.quoteJson = object.optJSONObject("quotedMessage") == null ? null : object.optJSONObject("quotedMessage").toString();

            arrayList.add(0, chatModel);
            adapter.notifyItemInserted(0);
            viewBinding.recyclerView.scrollToPosition(0);

            Yuwee.getInstance().getChatManager().markSingleMessageAsReadInRoom(roomId, chatModel.messageId);
        }
    }

    @Override
    public void onUserTypingInRoom(JSONObject jsonObject) {
        Utils.log("onUserTypingInRoom " + jsonObject);
        if (roomId.equalsIgnoreCase(jsonObject.optJSONObject("dataToSend").optString("roomId"))
                && !PrefUtils.getInstance().getUserLogin().result.user.id.equalsIgnoreCase(jsonObject.optJSONObject("dataToSend").optString("senderId"))) {

            showTyping();
        }
    }

    private void showTyping() {

        if (handler != null) {
            handler.removeCallbacks(runnable);
        } else {
            handler = new Handler();
        }

        handler.postDelayed(runnable, 1000);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("Typing...");
        }


    }

    @Override
    public void onMessageDeleted(JSONObject jsonObject) {
        Utils.log("onMessageDeleted " + jsonObject);
        if (roomId.equalsIgnoreCase(jsonObject.optJSONObject("group").optString("roomId"))
                && !PrefUtils.getInstance().getUserLogin().result.user.id.equalsIgnoreCase(jsonObject.optString("senderId"))) {
            String messageId = jsonObject.optString("messageId");
            for (int i = 0; i < arrayList.size(); i++) {
                if (arrayList.get(i).messageId.equalsIgnoreCase(messageId)) {
                    arrayList.remove(i);
                    adapter.notifyItemRemoved(i);
                    totalCount--;
                    break;
                }
            }
        }
    }

    @Override
    public void onMessageDeliverySuccess(JSONObject object) {
        Utils.log("onMessageDeliverySuccess " + object);


        if (roomId.equalsIgnoreCase(object.optString("roomId"))) {

            for (int i = 0; i < arrayList.size(); i++) {
                if (!TextUtils.isEmpty(arrayList.get(i).messageIdentifier) && arrayList.get(i).messageIdentifier.equalsIgnoreCase(object.optString("browserMessageId"))) {
                    arrayList.get(i).messageId = object.optString("messageId");
                    return;
                }
            }

            ChatModel chatModel = new ChatModel(Parcel.obtain());

            if (object.optString("messageType").equalsIgnoreCase("CALL")) {
                chatModel.viewType = ViewType.CALL;
                chatModel.callData = new ChatModel.CallData(Parcel.obtain());
                chatModel.callData.callerId = object.optJSONObject("sender").optString("_id");

                if (object.optJSONObject("sender").optString("senderId").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.callData.calleeName = name;
                    chatModel.callData.callerName = object.optJSONObject("sender").optString("name");
                } else {
                    chatModel.callData.calleeName = object.optJSONObject("sender").optString("name");
                    chatModel.callData.callerName = name;
                }
            } else if (object.optString("messageType").equalsIgnoreCase("FILE")) {
                if (object.optJSONObject("sender").optString("senderId").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_FILE;
                } else {
                    chatModel.viewType = ViewType.OTHER_FILE;
                }

                chatModel.fileData = new ChatModel.FileData(Parcel.obtain());
                chatModel.fileData.fileId = object.optJSONObject("fileInfo").optString("_id");
                chatModel.fileData.fileKey = object.optJSONObject("fileInfo").optString("fileKey");
                chatModel.fileData.fileName = object.optJSONObject("fileInfo").optString("fileName");

            } else {
                if (object.optJSONObject("sender").optString("senderId").equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
                    chatModel.viewType = ViewType.MY_MESSAGE;
                } else {
                    chatModel.viewType = ViewType.OTHERS_MESSAGE;
                }
                chatModel.message = object.optString("message");
            }

            chatModel.name = object.optJSONObject("sender").optString("name");
            chatModel.messageId = object.optString("messageId");
            chatModel.messageTime = String.valueOf(object.optLong("dateOfCreation"));
            chatModel.isForwarded = object.optBoolean("isForwarded");

            arrayList.add(0, chatModel);
            adapter.notifyItemInserted(0);
            viewBinding.recyclerView.scrollToPosition(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppData.activeRoomId = null;
        ChatImpl.getInstance().removeChatListener(this);
    }

    private void showDeleteDialog(final int position) {

        final ArrayList<String> mList = new ArrayList<>();
        mList.add("Delete for me");

        if (this.arrayList.get(position).viewType == ViewType.MY_MESSAGE || this.arrayList.get(position).viewType == ViewType.MY_FILE) {
            mList.add("Delete for everyone");
        }

        mList.add("Forward");
        mList.add("Quote");

        final String[] mData = new String[mList.size()];

        for (int i = 0; i < mList.size(); i++) {
            mData[i] = mList.get(i);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Options");
        builder.setItems(mData, (dialog, item) -> {
            dialog.dismiss();
            if (item == 0) {
                deleteForMe(position);
            } else {
                if (mData[item].equalsIgnoreCase("Delete for everyone")) {
                    deleteForEveryone(position);
                } else if (mData[item].equalsIgnoreCase("Forward")) {
                    /*if (arrayList.get(position).viewType == ViewType.MY_FILE || arrayList.get(position).viewType == ViewType.OTHER_FILE) {
                        Utils.showToast("Not available");
                        return;
                    }*/
                    Intent intent = new Intent(ChatDetailsActivity.this, ContactActivity.class);
                    intent.putExtra(ContactActivity.IS_FORWARD, true);
                    intent.putExtra(ContactActivity.CHAT_MODEL, arrayList.get(position));
                    startActivity(intent);
                } else if (mData[item].equalsIgnoreCase("Quote")) {
                    setUpQuoteView(position);
                }
            }
        });
        builder.show();
    }

    private void setUpQuoteView(int position) {

        quoteChatModel = arrayList.get(position);
        if (quoteChatModel.viewType == ViewType.MY_FILE || quoteChatModel.viewType == ViewType.OTHER_FILE) {
            viewBinding.ivQuoteImage.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(quoteChatModel.fileData.fileUrl)
                    .into(viewBinding.ivQuoteImage);
        } else {
            viewBinding.ivQuoteImage.setVisibility(View.GONE);
        }

        viewBinding.tvQuoteName.setText(quoteChatModel.name);
        viewBinding.tvQuoteMessage.setText(quoteChatModel.message);
        viewBinding.ivQuoteCross.setOnClickListener(v -> {
            viewBinding.llQuote.setVisibility(View.GONE);
            isQuoteOn = false;
        });
        viewBinding.llQuote.setVisibility(View.VISIBLE);
        isQuoteOn = true;
    }

    private void deleteForEveryone(final int position) {
        DialogUtils.showDialog(this, "Deleting message for everyone...");
        Yuwee.getInstance().getChatManager().deleteMessageInRoom(
                roomId,
                arrayList.get(position).messageId,
                MessageDeleteType.FOR_ALL, new OnDeleteMessageInRoomListener() {
                    @Override
                    public void onDeleteMessageInRoomSuccess(JSONObject jsonObject) {
                        DialogUtils.cancelDialog(ChatDetailsActivity.this);
                        if (jsonObject.optString("status").equalsIgnoreCase("success")) {
                            totalCount--;
                            arrayList.remove(position);
                            adapter.notifyItemRemoved(position);
                        } else {

                        }
                    }

                    @Override
                    public void onDeleteMessageInRoomFailed(String s) {
                        DialogUtils.cancelDialog(ChatDetailsActivity.this);
                    }
                });
    }

    private void deleteForMe(final int position) {
        DialogUtils.showDialog(this, "Deleting message...");
        Yuwee.getInstance().getChatManager().deleteMessageInRoom(
                roomId,
                arrayList.get(position).messageId,
                MessageDeleteType.FOR_ME, new OnDeleteMessageInRoomListener() {
                    @Override
                    public void onDeleteMessageInRoomSuccess(JSONObject jsonObject) {
                        DialogUtils.cancelDialog(ChatDetailsActivity.this);
                        if (jsonObject.optString("status").equalsIgnoreCase("success")) {
                            totalCount--;
                            arrayList.remove(position);
                            adapter.notifyItemRemoved(position);
                        } else {

                        }
                    }

                    @Override
                    public void onDeleteMessageInRoomFailed(String s) {
                        DialogUtils.cancelDialog(ChatDetailsActivity.this);
                    }
                });
    }

    @Override
    public void PickiTonStartListener() {
        DialogUtils.showDialog(this, "Please wait...");
    }

    @Override
    public void PickiTonProgressUpdate(int progress) {

    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        DialogUtils.cancelDialog(this);
        if (wasSuccessful) {

            String ex = path.substring(path.lastIndexOf("."));
            String ext = ex.substring(1);

            Yuwee.getInstance().getChatManager().getFileManager().sendFile(
                    roomId,
                    UUID.randomUUID().toString(),
                    path,
                    UUID.randomUUID().toString(),
                    ext,
                    new SendFileListener() {
                        @Override
                        public void onSendFileSuccess() {
                            Utils.log("File Upload Success.");
                        }

                        @Override
                        public void onSendFileFailed(@NotNull String errorMessage) {
                            Utils.log("File Upload Failed.");
                        }

                        @Override
                        public void onUploadFileProgress(int progress) {
                            Utils.log(progress);
                        }
                    });
        }
    }
}
