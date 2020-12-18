package com.yuwee.yuweesdkdemo.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;

public class DialogUtils {
    private static ProgressDialog dialog = null;

    public static void showDialog(final Context context, String message) {

        new Handler().postDelayed(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.setOnCancelListener(dialog -> {

                });
            }
        }, 5000);

        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }

        dialog = new ProgressDialog(context);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public static void cancelDialog(Activity activity) {
        if (activity.isFinishing()){
            dialog = null;
            return;
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
