package com.yuwee.yuweesdkdemo.activity;


import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityRegisterBinding;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnCreateUserListener;
import com.yuwee.sdk.model.user_reg.UserRegistrationResponse;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding viewBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Register");
        }

    }

    public void onRegisterClicked(View view) {
        if (viewBinding.etName.getText().toString().trim().length() < 3) {
            viewBinding.tilName.setError("Name must be at least of 3 characters.");
            return;
        }
        viewBinding.tilName.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(viewBinding.etEmail.getText().toString().trim()).matches()) {
            viewBinding.tilEmail.setError("Please enter valid email.");
            return;
        }
        viewBinding.tilEmail.setError(null);

        if (viewBinding.etPass.getText().toString().trim().length() < 6) {
            viewBinding.tilPass.setError("Password must be at least of 6 characters.");
            return;
        }
        viewBinding.tilPass.setError(null);

        DialogUtils.showDialog(this, "Registering..");

        Yuwee.getInstance().getUserManager().createUser(viewBinding.etName.getText().toString().trim(),
                viewBinding.etEmail.getText().toString().trim(),
                viewBinding.etPass.getText().toString().trim()
                , new OnCreateUserListener() {
                    @Override
                    public void onAccountCreateSuccess(UserRegistrationResponse userRegistrationResponse) {
                        Utils.showToast("Registration successful.");
                        DialogUtils.cancelDialog(RegisterActivity.this);
                        onBackPressed();
                    }

                    @Override
                    public void onAccountCreateFailure(UserRegistrationResponse userRegistrationResponse) {
                        Utils.showToast(userRegistrationResponse.message);
                        DialogUtils.cancelDialog(RegisterActivity.this);
                    }
                });
    }

    public void gotoLogin(View view) {
        onBackPressed();
    }
}
