package com.yuwee.yuweesdkdemo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.yuwee.yuweesdkdemo.R;
import com.yuwee.yuweesdkdemo.databinding.ActivityLoginBinding;
import com.yuwee.yuweesdkdemo.utils.DialogUtils;
import com.yuwee.yuweesdkdemo.utils.PrefUtils;
import com.yuwee.yuweesdkdemo.utils.Utils;
import com.yuwee.sdk.Yuwee;
import com.yuwee.sdk.listener.OnCreateSessionListener;
import com.yuwee.sdk.listener.OnCreateSessionViaTokenListener;
import com.yuwee.sdk.listener.OnRegisterFirebaseTokenListener;
import com.yuwee.sdk.model.InitParam;
import com.yuwee.sdk.model.user_login.User;
import com.yuwee.sdk.model.user_login.UserLoginResponse;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private ActivityLoginBinding viewBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Login");
        }

        if (Yuwee.getInstance().getUserManager().isLoggedIn()) {
            openOptionActivity();
        }

    }

    public void onLoginClicked(View view) {
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

        DialogUtils.showDialog(this, "Logging in..");
        Yuwee.getInstance()
                .getUserManager().createSessionViaCredentials(
                viewBinding.etEmail.getText().toString().trim(),
                viewBinding.etPass.getText().toString().trim(),
                "200000", new OnCreateSessionListener() {
                    @Override
                    public void onSessionCreateSuccess(UserLoginResponse userLoginResponse) {
                        PrefUtils.getInstance().saveLogin(userLoginResponse);
                        secondLoginMethod(userLoginResponse.result.user, userLoginResponse.accessToken);
                        //openOptionActivity();
                        //getToken();
                    }

                    @Override
                    public void onSessionCreateFailure(UserLoginResponse userLoginResponse) {
                        Utils.showToast(userLoginResponse.message);
                        DialogUtils.cancelDialog(LoginActivity.this);
                    }
                });

    }

    private void secondLoginMethod(User user, String accessToken) {
        InitParam initParam = new InitParam();
        initParam.accessToken = accessToken;
        try {
            initParam.userInfo = new JSONObject(new Gson().toJson(user));
            Yuwee.getInstance().getUserManager().createSessionViaToken(initParam, new OnCreateSessionViaTokenListener() {
                @Override
                public void onCreateSessionViaTokenSuccess() {
                    getToken();
                }

                @Override
                public void onCreateSessionViaTokenError(String error) {
                    Utils.log(error);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void getToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        DialogUtils.cancelDialog(LoginActivity.this);
                        Utils.showToast("An error occurred, please try again later.");
                        finish();
                        return;
                    }

                    String token = task.getResult().getToken();
                    Utils.log("Token: " + token);
                    registerFcmToken(token);
                });


    }

    private void registerFcmToken(String token) {
        Yuwee.getInstance().getUserManager().registerFirebaseToken(token, new OnRegisterFirebaseTokenListener() {
            @Override
            public void onRegisterTokenSuccess(String s) {
                DialogUtils.cancelDialog(LoginActivity.this);
                openOptionActivity();
            }

            @Override
            public void onRegisterTokenFail(String s) {
                DialogUtils.cancelDialog(LoginActivity.this);
                Utils.showToast("An error occurred, please try again later.");
                finish();
            }
        });
    }

    private void openOptionActivity() {
        askPermission();
    }

    private void askPermission() {
        int hasCameraAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasAudioAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int hasStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasCameraAccess == PackageManager.PERMISSION_GRANTED && hasAudioAccess == PackageManager.PERMISSION_GRANTED && hasStoragePermission == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Utils.showToast("Please allow all permission to continue.");
                    finish();
                }
                break;
        }
    }

    public void gotoRegister(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
