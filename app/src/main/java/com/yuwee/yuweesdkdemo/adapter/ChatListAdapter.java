package com.yuwee.yuweesdkdemo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.LayoutChatSingleRowBinding;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.model.ViewType;
import com.yuwee.yuweesdkdemo.utils.Utils;

import java.util.ArrayList;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.MyViewHolder> {

    private ArrayList<ChatModel> arrayList;
    private OnListItemClickListener listItemClickListener;

    public ChatListAdapter(ArrayList<ChatModel> arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ChatListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutChatSingleRowBinding viewBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_chat_single_row, parent, false);
        return new MyViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.MyViewHolder holder, int position) {
        holder.viewBinding.tvName.setText(arrayList.get(position).name);
        holder.viewBinding.tvTime.setText(Utils.formatTime(String.valueOf(arrayList.get(position).lastMessageTime)));
        if (arrayList.get(position).viewType == ViewType.CALL) {
            holder.viewBinding.tvLastMessage.setText(arrayList.get(position).name + " called you.");
        } else if (arrayList.get(position).viewType == ViewType.MY_FILE) {
            holder.viewBinding.tvLastMessage.setText("File");
        } else {
            holder.viewBinding.tvLastMessage.setText(arrayList.get(position).message);
        }
        if (arrayList.get(position).unreadCount > 0) {
            holder.viewBinding.tvCount.setVisibility(View.VISIBLE);
            holder.viewBinding.tvCount.setText(String.valueOf(arrayList.get(position).unreadCount));
        } else {
            holder.viewBinding.tvCount.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public void setOnListItemClickListener(OnListItemClickListener listItemClickListener) {
        this.listItemClickListener = listItemClickListener;
    }


    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private LayoutChatSingleRowBinding viewBinding;

        MyViewHolder(LayoutChatSingleRowBinding binding) {
            super(binding.getRoot());
            viewBinding = binding;
            viewBinding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (getLayoutPosition() != RecyclerView.NO_POSITION) {
                if (listItemClickListener != null) {
                    listItemClickListener.onListItemClicked(getLayoutPosition());
                }
            }
        }
    }

    public interface OnListItemClickListener {
        void onListItemClicked(int position);
    }
}
