package com.yuwee.yuweesdkdemo.adapter;

import android.annotation.SuppressLint;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.activity.ChatDetailsActivity;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowCallMessageBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowMyFileBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowMyMessageBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowOtherFileBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowOtherMessageBinding;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.model.ViewType;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.PrefUtils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.GetFileUrlListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ChatDetailsAdapter extends RecyclerView.Adapter<ChatDetailsAdapter.Holder> {

    private ArrayList<ChatModel> arrayList;

    private static final int MY_VIEW = 0;
    private static final int OTHER_VIEW = 1;
    private static final int CALL_VIEW = 2;
    private static final int MY_FILE_VIEW = 3;
    private static final int OTHER_FILE_VIEW = 4;

    private OnChatInteractListener listener;
    private Activity activity;

    public ChatDetailsAdapter(ArrayList<ChatModel> arrayList, Activity chatDetailsActivity) {
        this.arrayList = arrayList;
        this.activity = chatDetailsActivity;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        Holder holder = null;
        ViewDataBinding binding;
        switch (position) {
            case MY_VIEW:
                binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.layout_single_row_my_message, viewGroup, false);
                holder = new MyViewHolder(binding);
                break;
            case OTHER_VIEW:
                binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.layout_single_row_other_message, viewGroup, false);
                holder = new OtherViewHolder(binding);
                break;
            case CALL_VIEW:
                binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.layout_single_row_call_message, viewGroup, false);
                holder = new CallViewHolder(binding);
                break;
            case MY_FILE_VIEW:
                binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.layout_single_row_my_file, viewGroup, false);
                holder = new MyFileViewHolder(binding);
                break;
            case OTHER_FILE_VIEW:
                binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.layout_single_row_other_file, viewGroup, false);
                holder = new OtherFileViewHolder(binding);
                break;
        }
        return holder;

    }

    @Override
    public void onBindViewHolder(@NonNull Holder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case MY_VIEW:
                MyViewHolder holder = (MyViewHolder) viewHolder;
                setUpMyView(holder, position);
                break;

            case OTHER_VIEW:
                OtherViewHolder otherViewHolder = (OtherViewHolder) viewHolder;
                setUpOtherView(otherViewHolder, position);
                break;

            case CALL_VIEW:
                CallViewHolder callViewHolder = (CallViewHolder) viewHolder;
                setUpCallView(callViewHolder, position);
                break;

            case MY_FILE_VIEW:
                MyFileViewHolder myFileViewHolder = (MyFileViewHolder) viewHolder;
                setUpMyFileView(myFileViewHolder, position);
                break;
            case OTHER_FILE_VIEW:
                OtherFileViewHolder otherFileViewHolder = (OtherFileViewHolder) viewHolder;
                setUpOtherFileView(otherFileViewHolder, position);
                break;
        }
    }

    private void setUpMyFileView(MyFileViewHolder viewHolder, int position) {
        if (TextUtils.isEmpty(arrayList.get(position).fileData.fileUrl)) {
            viewHolder.binding.ivDownload.setVisibility(View.VISIBLE);
        } else {
            viewHolder.binding.ivDownload.setVisibility(View.GONE);
            Glide.with(viewHolder.binding.ivFile.getContext())
                    .load(arrayList.get(position).fileData.fileUrl)
                    .into(viewHolder.binding.ivFile);
        }


        viewHolder.binding.tvForward.setVisibility(arrayList.get(position).isForwarded ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(arrayList.get(position).quoteJson)) {
            try {
                JSONObject jsonObject = new JSONObject(arrayList.get(position).quoteJson);
                //holder.binding.tvQuoteName.setText(jsonObject.optJSONObject("senderInfo").optString("name"));
                //holder.binding.tvQuoteText.setVisibility(View.GONE);
                viewHolder.binding.llQuote.setVisibility(View.VISIBLE);

                if (jsonObject.optString("messageType").equalsIgnoreCase("file")) {
                    Glide.with(viewHolder.binding.ivQuoteImage.getContext())
                            .load(jsonObject.optString("downloadUrl"))
                            .into(viewHolder.binding.ivQuoteImage);
                    viewHolder.binding.ivQuoteImage.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.binding.tvQuoteMessage.setVisibility(View.VISIBLE);
                    viewHolder.binding.tvQuoteMessage.setText(jsonObject.optString("content"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            viewHolder.binding.llQuote.setVisibility(View.GONE);
        }
    }

    private void setUpOtherFileView(OtherFileViewHolder viewHolder, int position) {

        if (TextUtils.isEmpty(arrayList.get(position).fileData.fileUrl)) {
            viewHolder.binding.ivDownload.setVisibility(View.VISIBLE);
        } else {
            viewHolder.binding.ivDownload.setVisibility(View.GONE);
            Glide.with(viewHolder.binding.ivFile.getContext())
                    .load(arrayList.get(position).fileData.fileUrl)
                    .into(viewHolder.binding.ivFile);
        }

        viewHolder.binding.tvForward.setVisibility(arrayList.get(position).isForwarded ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(arrayList.get(position).quoteJson)) {
            try {
                JSONObject jsonObject = new JSONObject(arrayList.get(position).quoteJson);
                //holder.binding.tvQuoteName.setText(jsonObject.optJSONObject("senderInfo").optString("name"));
                //holder.binding.tvQuoteText.setVisibility(View.GONE);
                viewHolder.binding.llQuote.setVisibility(View.VISIBLE);

                if (jsonObject.optString("messageType").equalsIgnoreCase("file")) {
                    Glide.with(viewHolder.binding.ivQuoteImage.getContext())
                            .load(jsonObject.optString("downloadUrl"))
                            .into(viewHolder.binding.ivQuoteImage);
                    viewHolder.binding.ivQuoteImage.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.binding.tvQuoteText.setVisibility(View.VISIBLE);
                    viewHolder.binding.tvQuoteText.setText(jsonObject.optString("content"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            viewHolder.binding.llQuote.setVisibility(View.GONE);
        }
    }

    private void setUpMyView(MyViewHolder holder, int position) {
        holder.binding.tvMsg.setText(arrayList.get(position).message);
        loadNextPage(position);
        holder.binding.tvForward.setVisibility(arrayList.get(position).isForwarded ? View.VISIBLE : View.GONE);
        if (!TextUtils.isEmpty(arrayList.get(position).quoteJson)) {
            try {
                JSONObject jsonObject = new JSONObject(arrayList.get(position).quoteJson);
                //holder.binding.tvQuoteName.setText(jsonObject.optJSONObject("senderInfo").optString("name"));
                //holder.binding.tvQuoteText.setVisibility(View.GONE);
                holder.binding.llQuote.setVisibility(View.VISIBLE);

                if (jsonObject.optString("messageType").equalsIgnoreCase("file")) {
                    Glide.with(holder.binding.ivQuoteImage.getContext())
                            .load(jsonObject.optString("downloadUrl"))
                            .into(holder.binding.ivQuoteImage);
                    holder.binding.ivQuoteImage.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.tvQuoteText.setVisibility(View.VISIBLE);
                    holder.binding.tvQuoteText.setText(jsonObject.optString("content"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            holder.binding.llQuote.setVisibility(View.GONE);
        }
    }

    private void setUpOtherView(OtherViewHolder holder, int position) {
        holder.binding.tvMsg.setText(arrayList.get(position).message);
        loadNextPage(position);
        holder.binding.tvForward.setVisibility(arrayList.get(position).isForwarded ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(arrayList.get(position).quoteJson)) {
            try {
                JSONObject jsonObject = new JSONObject(arrayList.get(position).quoteJson);
                //holder.binding.tvQuoteName.setText(jsonObject.optJSONObject("senderInfo").optString("name"));
                //holder.binding.tvQuoteText.setVisibility(View.GONE);
                holder.binding.llQuote.setVisibility(View.VISIBLE);

                if (jsonObject.optString("messageType").equalsIgnoreCase("file")) {
                    Glide.with(holder.binding.ivQuoteImage.getContext())
                            .load(jsonObject.optString("downloadUrl"))
                            .into(holder.binding.ivQuoteImage);
                    holder.binding.ivQuoteImage.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.tvQuoteText.setVisibility(View.VISIBLE);
                    holder.binding.tvQuoteText.setText(jsonObject.optString("content"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            holder.binding.llQuote.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setUpCallView(CallViewHolder holder, int position) {
        if (arrayList.get(position).callData.callerId.equalsIgnoreCase(PrefUtils.getInstance().getUserLogin().result.user.id)) {
            holder.binding.tvCallMessage.setText("You called " + arrayList.get(position).callData.calleeName);
        } else {
            holder.binding.tvCallMessage.setText(arrayList.get(position).callData.callerName + " called you");
        }

        loadNextPage(position);
    }

    private void loadNextPage(int position) {
        if (position == arrayList.size() - 1) {
            if (listener != null) {
                listener.onLoadNextPage();
            }
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return arrayList.get(position).viewType.type;
    }

    public void setOnItemInteractListener(OnChatInteractListener listener1) {
        listener = listener1;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        Holder(@NonNull ViewDataBinding binding) {
            super(binding.getRoot());
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClickListener(getLayoutPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                if (arrayList.get(getLayoutPosition()).viewType != ViewType.CALL) {
                    listener.onItemLongClickListener(getLayoutPosition());
                }
            }
            return true;
        }
    }

    private class MyViewHolder extends Holder {

        private LayoutSingleRowMyMessageBinding binding;

        MyViewHolder(@NonNull ViewDataBinding mBinding) {
            super(mBinding);
            binding = (LayoutSingleRowMyMessageBinding) mBinding;
        }


    }

    private class OtherViewHolder extends Holder {

        private LayoutSingleRowOtherMessageBinding binding;

        OtherViewHolder(@NonNull ViewDataBinding mBinding) {
            super(mBinding);
            binding = (LayoutSingleRowOtherMessageBinding) mBinding;
        }
    }

    private class CallViewHolder extends Holder {

        private LayoutSingleRowCallMessageBinding binding;

        CallViewHolder(@NonNull ViewDataBinding mBinding) {
            super(mBinding);
            binding = (LayoutSingleRowCallMessageBinding) mBinding;
        }
    }

    private class MyFileViewHolder extends Holder implements View.OnClickListener {

        private final LayoutSingleRowMyFileBinding binding;

        MyFileViewHolder(@NonNull ViewDataBinding mBinding) {
            super(mBinding);
            binding = (LayoutSingleRowMyFileBinding) mBinding;
            binding.ivDownload.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.iv_download:
                    getFileUrl(getLayoutPosition());
                    break;
            }
        }
    }

    private void getFileUrl(int position){
        DialogUtils.showDialog(activity, "Please wait...");
        Yuwee.getInstance().getChatManager().getFileManager().getFileUrl(
                arrayList.get(position).fileData.fileId,
                arrayList.get(position).fileData.fileKey,
                new GetFileUrlListener() {
                    @Override
                    public void onDownloadFileSuccess(@NotNull String fileUrl) {
                        arrayList.get(position).fileData.fileUrl = fileUrl;
                        notifyItemChanged(position);
                        DialogUtils.cancelDialog(activity);
                    }

                    @Override
                    public void onDownloadFileFailed(@NotNull String errorMessage) {
                        DialogUtils.cancelDialog(activity);
                    }
                }
        );
    }

    private class OtherFileViewHolder extends Holder implements View.OnClickListener {

        private final LayoutSingleRowOtherFileBinding binding;

        OtherFileViewHolder(@NonNull ViewDataBinding mBinding) {
            super(mBinding);
            binding = (LayoutSingleRowOtherFileBinding) mBinding;
            binding.ivDownload.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.iv_download:
                    getFileUrl(getLayoutPosition());
                    break;
            }
        }
    }

    public interface OnChatInteractListener {
        void onItemClickListener(int position);

        void onItemLongClickListener(int position);

        void onLoadNextPage();
    }
}
