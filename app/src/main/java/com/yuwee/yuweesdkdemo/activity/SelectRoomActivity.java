package com.yuwee.yuweesdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivitySelectRoomBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleSelectRoomBinding;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnFetchChatListListener;
import com.yuwee.sdk.model.chat_fetch_list.ChatList;
import com.yuwee.sdk.model.chat_fetch_list.YuweeChatListModel;

import java.util.ArrayList;
import java.util.List;

public class SelectRoomActivity extends AppCompatActivity {

    private List<ChatList> arrayList = new ArrayList<>();
    private ActivitySelectRoomBinding viewBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_select_room);
        getChatList();
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.recyclerView.setAdapter(new RoomAdapter());
    }

    private void getChatList() {

        DialogUtils.showDialog(this, "Loading data...");
        Yuwee.getInstance().getChatManager().fetchChatList(new OnFetchChatListListener() {
            @Override
            public void onFetchChatListSuccess(YuweeChatListModel yuWeeChatListModel) {

                if (yuWeeChatListModel.status.equalsIgnoreCase("success")) {
                    arrayList = yuWeeChatListModel.result.results;
                    viewBinding.recyclerView.getAdapter().notifyDataSetChanged();
                }

                DialogUtils.cancelDialog(SelectRoomActivity.this);
            }

            @Override
            public void onFetchChatListFailed(String s) {
                DialogUtils.cancelDialog(SelectRoomActivity.this);
            }
        });
    }

    private class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {


        @NonNull
        @Override
        public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutSingleSelectRoomBinding viewBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_single_select_room, parent, false);
            return new RoomViewHolder(viewBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
            holder.viewBinding.tvName.setText(arrayList.get(position).name);
            holder.viewBinding.tvRoomId.setText(arrayList.get(position).roomId);
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        private class RoomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private LayoutSingleSelectRoomBinding viewBinding;

            RoomViewHolder(@NonNull LayoutSingleSelectRoomBinding itemView) {
                super(itemView.getRoot());
                this.viewBinding = itemView;
                itemView.getRoot().setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (getLayoutPosition() != RecyclerView.NO_POSITION){
                    Intent intent = new Intent();
                    intent.putExtra("name", arrayList.get(getLayoutPosition()).name);
                    intent.putExtra("roomId", arrayList.get(getLayoutPosition()).roomId);

                    setResult(CommonStatusCodes.SUCCESS, intent);
                    finish();
                }
            }
        }
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
}
