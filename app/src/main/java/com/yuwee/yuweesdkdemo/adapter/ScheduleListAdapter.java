package com.yuwee.yuweesdkdemo.adapter;

import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.LayoutScheduleListSingleRowBinding;
import com.yuwee.yuweesdkdemo.model.ScheduleModel;
import com.yuwee.yuweesdkdemo.utils.Utils;

import java.util.ArrayList;

public class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.MyViewHolder> {

    private ArrayList<ScheduleModel> arrayList;
    private OnListItemClickListener listItemClickListener;

    public ScheduleListAdapter(ArrayList<ScheduleModel> arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ScheduleListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutScheduleListSingleRowBinding viewBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_schedule_list_single_row, parent, false);
        return new MyViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleListAdapter.MyViewHolder holder, int position) {
        holder.viewBinding.tvName.setText(arrayList.get(position).groupName);
        holder.viewBinding.tvTime.setText(Utils.formatTime(arrayList.get(position).meetingDate));
        if (!TextUtils.isEmpty(arrayList.get(position).callId)) {
            holder.viewBinding.btnJoin.setVisibility(View.VISIBLE);
        } else {
            holder.viewBinding.btnJoin.setVisibility(View.GONE);
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

        private LayoutScheduleListSingleRowBinding viewBinding;

        MyViewHolder(LayoutScheduleListSingleRowBinding binding) {
            super(binding.getRoot());
            viewBinding = binding;
            viewBinding.btnJoin.setOnClickListener(this);
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
