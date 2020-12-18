package com.yuwee.yuweesdkdemo.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.ChatDetailsActivity;
import com.yuwee.yuweesdkdemo.adapter.ChatListAdapter;
import com.yuwee.yuweesdkdemo.databinding.FragmentDirectChatBinding;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.model.ViewType;
import com.yuwee.yuweesdkdemo.singleton.ChatImpl;
import com.yuwee.sdk.model.chat_fetch_list.YuweeChatListModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class DirectChatFragment extends Fragment implements ChatListAdapter.OnListItemClickListener, ChatImpl.UpdateCountListener {


    private ChatListAdapter adapter = null;
    private ArrayList<ChatModel> arrayList = new ArrayList<>();

    public DirectChatFragment() {
        // Required empty public constructor
    }

    private FragmentDirectChatBinding viewBinding = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        viewBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_direct_chat, container, false);

        setUpRecyclerView();
        ChatImpl.getInstance().setUpdateCountListener(this);

        return viewBinding.getRoot();
    }

    private void setUpRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        viewBinding.recyclerView.setLayoutManager(manager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), manager.getOrientation());
        viewBinding.recyclerView.addItemDecoration(dividerItemDecoration);

        adapter = new ChatListAdapter(arrayList);
        adapter.setOnListItemClickListener(this);
        viewBinding.recyclerView.setAdapter(adapter);
    }

    public void update(YuweeChatListModel yuWeeChatListModel) {
        for (int i = 0; i < yuWeeChatListModel.result.results.size(); i++) {
            if (!yuWeeChatListModel.result.results.get(i).isGroupChat) {
                ChatModel chatModel = new ChatModel(Parcel.obtain());

                if (yuWeeChatListModel.result.results.get(i).lastMessage.messageType.equalsIgnoreCase("text")) {
                    chatModel.viewType = ViewType.MY_MESSAGE;
                    chatModel.message = yuWeeChatListModel.result.results.get(i).lastMessage.message;
                } else if (yuWeeChatListModel.result.results.get(i).lastMessage.messageType.equalsIgnoreCase("call")) {
                    chatModel.callData = new ChatModel.CallData(Parcel.obtain());
                    chatModel.viewType = ViewType.CALL;
                    chatModel.callData.calleeName = chatModel.message = yuWeeChatListModel.result.results.get(i).lastMessage.senderInfo.name;
                } else {
                    chatModel.viewType = ViewType.MY_FILE;
                }
                chatModel.unreadCount = yuWeeChatListModel.result.results.get(i).unreadMessageCount;
                chatModel.name = yuWeeChatListModel.result.results.get(i).name;
                chatModel.roomId = yuWeeChatListModel.result.results.get(i).roomId;
                chatModel.lastMessageTime = yuWeeChatListModel.result.results.get(i).lastMessage.messageTime;

                arrayList.add(chatModel);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClicked(int position) {
        Intent intent = new Intent(getContext(), ChatDetailsActivity.class);
        intent.putExtra(ChatDetailsActivity.ROOM_ID, arrayList.get(position).roomId);
        intent.putExtra(ChatDetailsActivity.NAME, arrayList.get(position).name);
        intent.putExtra(ChatDetailsActivity.IS_GROUP_CHAT, false);
        startActivity(intent);
    }

    public void updateNow(ChatModel chatModel, boolean isNewMessage) {
        boolean isFound = false;
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).roomId.equalsIgnoreCase(chatModel.roomId)) {
                if (isNewMessage) {
                    chatModel.unreadCount++;
                }
                chatModel.name = arrayList.get(i).name;
                arrayList.set(i, chatModel);
                if (i != 0 && arrayList.size() > 1) {
                    ChatModel cc = arrayList.get(0);
                    arrayList.set(0, chatModel);
                    arrayList.set(i, cc);
                }
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            arrayList.add(0, chatModel);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClearCount(String roomId) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).roomId.equalsIgnoreCase(roomId)) {
                arrayList.get(i).unreadCount = 0;
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }
}
