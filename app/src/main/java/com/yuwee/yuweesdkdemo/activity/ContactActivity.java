package com.yuwee.yuweesdkdemo.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityContactBinding;
import com.yuwee.yuweesdkdemo.databinding.LayoutContactSingleRowBinding;
import com.yuwee.yuweesdkdemo.model.ChatModel;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.enums.Status;
import com.yuwee.sdk.listener.OnAddContactListener;
import com.yuwee.sdk.listener.OnContactStatusChangeListener;
import com.yuwee.sdk.listener.OnDeleteContactListener;
import com.yuwee.sdk.listener.OnFetchAllContactsStatusListener;
import com.yuwee.sdk.listener.OnFetchChatRoomListener;
import com.yuwee.sdk.listener.OnFetchContactListListener;
import com.yuwee.sdk.listener.OnFetchContactListener;
import com.yuwee.sdk.listener.OnFetchUserStatusListener;
import com.yuwee.sdk.model.ContactData;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class ContactActivity extends AppCompatActivity {

    public static final String IS_FORWARD = "is_forward";
    public static final String CHAT_MODEL = "chat_model";
    private ActivityContactBinding viewBinding = null;
    private ContactAdapter adapter = null;
    private ArrayList<Contacts> arrayList = new ArrayList<>();
    private boolean isForward;
    private ChatModel chatModel;
    private int positionToDelete = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_contact);

        if (getIntent() != null && getIntent().getExtras() != null && getIntent().hasExtra(IS_FORWARD)) {
            isForward = true;
            chatModel = getIntent().getExtras().getParcelable(CHAT_MODEL);
        }

        viewBinding.favForward.hide();
        //viewBinding.favForward.setOnClickListener(v -> forwardChat());
        setUpToolbar();

        setUpRecyclerView();

        getContactList();

        Yuwee.getInstance().getStatusManager().setContactStatusChangeListener(new OnContactStatusChangeListener() {
            @Override
            public void onContactStatusChange(JSONObject jsonObject) {
                Utils.log(jsonObject);
            }
        });

        Yuwee.getInstance().getStatusManager().fetchAllContactsStatus(new OnFetchAllContactsStatusListener() {
            @Override
            public void onFetchAllContactsStatusSuccess(JSONObject jsonObject) {

            }

            @Override
            public void onFetchAllContactsStatusFailed(String error) {

            }
        });

    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Contact");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    private void setUpRecyclerView() {
        adapter = new ContactAdapter();
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.recyclerView.setAdapter(adapter);
        viewBinding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

/*
    private void forwardChat() {

        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).isChecked) {
                forwardNow(i);
            }
        }

        Utils.showToast("Message will be forwarded.");
        onBackPressed();

    }

    private void forwardNow(int position) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(this.arrayList.get(position).email);
        Yuwee.getInstance().getChatManager().fetchChatRoomByEmails(arrayList, true, new OnFetchChatRoomListener() {
            @Override
            public void onFetchChatRoomSuccess(@NotNull JSONObject jsonObject) {
                try {
                    String roomId = jsonObject.getJSONObject("result").getJSONObject("room").getString("_id");
                    if (chatModel.messageType.equalsIgnoreCase("text")) {
                        Yuwee.getInstance().getChatManager().forwardMessage(chatModel.message, roomId, String.valueOf(System.currentTimeMillis()));
                    } else if (chatModel.messageType.equalsIgnoreCase("file")) {
                        JSONObject object = new JSONObject();


                        object.put("fileName", "google.png");
                        object.put("fileExtension", "png");
                        object.put("fileSize", "200");
                        object.put("downloadUrl", chatModel.fileData.fileUrl);
                        // OLD: forward file is removed from sdk
                        Yuwee.getInstance().getChatManager().forwardFile(object, roomId, String.valueOf(System.currentTimeMillis()));

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFetchChatRoomFailed(String error) {
            }
        });
    }*/

    private void showActionDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Option");
        builder.setItems(new String[]{"Fetch Details", "Delete", "Get Status"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    fetchContactDetails(arrayList.get(position).id);
                    break;
                case 1:
                    positionToDelete = position;
                    deleteContact(arrayList.get(position).id);
                    break;
                case 2:
                    getStatusByContactId(arrayList.get(position).id);
                    break;
            }
        });
        builder.show();
    }

    private void getContactList() {
        DialogUtils.showDialog(this, "Please wait...");
        Yuwee.getInstance().getContactManager().fetchContactList(new OnFetchContactListListener() {
            @Override
            public void onFetchContactListSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ContactActivity.this);
                JSONArray result = jsonObject.optJSONArray("result");
                for (int i = 0; i < result.length(); i++) {
                    Contacts contacts = new Contacts();
                    contacts.name = result.optJSONObject(i).optString("name");
                    contacts.email = result.optJSONObject(i).optString("email");
                    contacts.id = result.optJSONObject(i).optString("_id");

                    arrayList.add(contacts);
                }
                sort();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFetchContactListFailed(String error) {
                DialogUtils.cancelDialog(ContactActivity.this);
            }
        });
    }

    private void sort() {
        Collections.sort(arrayList, (o1, o2) -> o1.name.compareTo(o2.name));
    }

    private void addContactDialog(String emailText, String nameText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add contact");
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.layout_add_contact_dialog, null);
        EditText email = view.findViewById(R.id.et_email);
        EditText name = view.findViewById(R.id.et_name);
        email.setText(emailText);
        name.setText(nameText);
        builder.setView(view);
        builder.setPositiveButton("Add", (dialog, which) -> {

            if (name.getText().toString().trim().length() < 3) {
                Toast.makeText(ContactActivity.this, "Name must be at least of 3 characters.", Toast.LENGTH_SHORT).show();
                addContactDialog(email.getText().toString().trim(), "");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()) {
                Toast.makeText(ContactActivity.this, "Email is not valid.", Toast.LENGTH_SHORT).show();
                addContactDialog("", name.getText().toString().trim());
                return;
            }

            addContactNow(name.getText().toString().trim(), email.getText().toString().trim());

        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });
        builder.setCancelable(false);
        builder.show();


    }

    public void addContact(View view) {
        addContactDialog("", "");
    }

    private void addContactNow(String name, String email) {
        ContactData contactData = new ContactData();
        contactData.email = email;
        contactData.name = name;
        DialogUtils.showDialog(this, "Adding contact...");
        Yuwee.getInstance().getContactManager().addContact(contactData, new OnAddContactListener() {
            @Override
            public void onContactAddSuccess(JSONObject response) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", response.toString());
                JSONObject result = response.optJSONObject("result");
                Contacts contacts = new Contacts();
                contacts.name = result.optString("name");
                contacts.email = result.optString("email");
                contacts.id = result.optString("_id");

                arrayList.add(contacts);

                sort();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onContactAddFailed(String response) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", response);
                Utils.showToast(response);
            }
        });
    }

    private void fetchContactDetails(String id) {
        DialogUtils.showDialog(this, "Fetching contact details...");
        Yuwee.getInstance().getContactManager().fetchContactDetails(id, new OnFetchContactListener() {
            @Override
            public void onFetchContactSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ContactActivity.this);
                showData(jsonObject.toString());
            }

            @Override
            public void onFetchContactFailed(String error) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", error);
            }
        });
    }

    private void showData(String data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView textView = new TextView(this);
        textView.setText(data);
        builder.setView(textView);
        builder.setPositiveButton("OK", (dialog, which) -> {

        });
        builder.show();
    }

    private void deleteContact(String id) {
        DialogUtils.showDialog(this, "Deleting contact...");
        Yuwee.getInstance().getContactManager().deleteContact(id, new OnDeleteContactListener() {

            @Override
            public void onContactDeleteSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", jsonObject.toString());
                if (jsonObject.optString("status").equalsIgnoreCase("success")) {
                    arrayList.remove(positionToDelete);
                    sort();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onContactDeleteFailed(String error) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", error);
            }
        });
    }

    public void changeStatus(View view) {
        Yuwee.getInstance().getStatusManager().setStatus(Status.ONLINE);
    }

    private void getStatusByContactId(String id) {
        DialogUtils.showDialog(this, "Fetching status...");
        Yuwee.getInstance().getStatusManager().getParticularUserStatusByContactId(id, new OnFetchUserStatusListener() {
            @Override
            public void onUserStatusFetchSuccess(JSONObject jsonObject) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Log.e("TANAY", jsonObject.toString());
                showData(jsonObject.toString());
            }

            @Override
            public void onUserStatusFetchFailed(String error) {
                DialogUtils.cancelDialog(ContactActivity.this);
                Toast.makeText(ContactActivity.this, error, Toast.LENGTH_SHORT).show();
                Log.e("TANAY", error);
            }
        });
    }

    private void calculate() {
        int count = 0;
        for (Contacts contacts :
                arrayList) {
            if (contacts.isChecked) {
                count++;
            }
        }
        if (count > 0) {
            viewBinding.favForward.show();
        } else {
            viewBinding.favForward.hide();
        }
    }

    private class Contacts {
        String name, email, id;
        boolean isChecked;
    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactHolder> {

        @NonNull
        @Override
        public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutContactSingleRowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_contact_single_row, parent, false);
            return new ContactHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactHolder holder, int position) {
            holder.binding.tvName.setText(arrayList.get(position).name);
            holder.binding.tvEmail.setText(arrayList.get(position).email);
            holder.binding.checkBox.setChecked(arrayList.get(position).isChecked);
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private LayoutContactSingleRowBinding binding;

            ContactHolder(@NonNull LayoutContactSingleRowBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                itemView.setOnClickListener(this);
                if (isForward) {
                    binding.checkBox.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClick(View v) {
                if (isForward) {
                    arrayList.get(getLayoutPosition()).isChecked = !arrayList.get(getLayoutPosition()).isChecked;
                    notifyItemChanged(getLayoutPosition());
                    calculate();
                } else {
                    showActionDialog(getLayoutPosition());
                }
            }
        }
    }
}
