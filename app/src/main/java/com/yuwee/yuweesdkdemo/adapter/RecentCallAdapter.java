package com.yuwee.yuweesdkdemo.adapter;

import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.LayoutRecentCallSingleRowBinding;
import com.yuwee.yuweesdkdemo.model.RecentCallModel;
import com.yuwee.yuweesdkdemo.utils.Utils;

import java.util.ArrayList;

public class RecentCallAdapter extends RecyclerView.Adapter<RecentCallAdapter.MyViewHolder> {

    private ArrayList<RecentCallModel> arrayList;
    private NextPageLoadListener listener;

    public RecentCallAdapter(ArrayList<RecentCallModel> arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public RecentCallAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutRecentCallSingleRowBinding viewBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_recent_call_single_row, parent, false);
        return new MyViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentCallAdapter.MyViewHolder holder, int position) {

        holder.viewBinding.tvName.setText(arrayList.get(position).confName);
        holder.viewBinding.tvTime.setText(Utils.formatTime(arrayList.get(position).callTime));
        holder.viewBinding.tvCallType.setText(arrayList.get(position).isVideoCall ? "Video Call" : "Audio Call");

        if (position == arrayList.size() - 1) {
            listener.loadNextPageNow();
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public void setNextPageLoadListener(NextPageLoadListener listener) {
        this.listener = listener;
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private LayoutRecentCallSingleRowBinding viewBinding;

        MyViewHolder(LayoutRecentCallSingleRowBinding binding) {
            super(binding.getRoot());
            viewBinding = binding;
        }
    }

    public interface NextPageLoadListener {
        void loadNextPageNow();
    }
}
