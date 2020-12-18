package com.yuwee.yuweesdkdemo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.yuwee.yuweesdkdemo.R
import com.yuwee.yuweesdkdemo.databinding.ActivityJoinMeetingBinding
import com.yuwee.yuweesdkdemo.utils.DialogUtils
import com.yuwee.yuweesdkdemo.utils.PrefUtils
import com.yuwee.yuweesdkdemo.utils.Utils
import com.yuwee.sdk.Yuwee
import com.yuwee.sdk.body.MeetingBody.JoinMedia
import com.yuwee.sdk.body.MeetingBody.RegisterMeetingBody
import com.yuwee.sdk.listener.OnMeetingCallback
import org.json.JSONException
import org.json.JSONObject

class JoinMeetingActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityJoinMeetingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_join_meeting)

        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(intent.extras!!.getString("DATA", ""))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        viewBinding.etMeetingId.setText(jsonObject!!.optString("meetingTokenId"))
        viewBinding.etMeetingPassword.setText(jsonObject.optString("passcode"))
        viewBinding.etUserId.setText(PrefUtils.getInstance().userLogin.result.user.email)
        viewBinding.etUserName.setText(PrefUtils.getInstance().userLogin.result.user.name)
        viewBinding.btnJoin.setOnClickListener {
            if (viewBinding.etMeetingId.text.toString().isEmpty()) {
                viewBinding.etMeetingId.error = "Meeting Id is not valid."
                viewBinding.etMeetingId.requestFocus()
                return@setOnClickListener
            }
            if (viewBinding.etMeetingPassword.text.toString().isEmpty()) {
                viewBinding.etMeetingPassword.error = "Meeting password is not valid."
                viewBinding.etMeetingPassword.requestFocus()
                return@setOnClickListener
            }
            if (viewBinding.etUserName.text.toString().isEmpty()) {
                viewBinding.etUserName.error = "Nick name is not valid."
                viewBinding.etUserName.requestFocus()
                return@setOnClickListener
            }
            if (viewBinding.etMeetingPassword.text.toString().isEmpty()) {
                viewBinding.etMeetingPassword.error = "Meeting password is not valid."
                viewBinding.etMeetingPassword.requestFocus()
                return@setOnClickListener
            }
            val body = RegisterMeetingBody()
            body.nickName = viewBinding.etUserName.text.toString()
            body.guestId = viewBinding.etUserId.text.toString()
            val joinMedia = JoinMedia()
            joinMedia.audio = true
            joinMedia.video = true
            body.joinMedia = joinMedia
            body.meetingTokenId = viewBinding.etMeetingId.text.toString()
            body.passcode = viewBinding.etMeetingPassword.text.toString()
            DialogUtils.showDialog(this, "Joining...")
            Yuwee.instance.meetingManager.registerInMeeting(body, object : OnMeetingCallback {
                override fun onSuccess(mObject: JSONObject) {
                    DialogUtils.cancelDialog(this@JoinMeetingActivity)
                    val intent = Intent(this@JoinMeetingActivity, MeetingCallActivity::class.java)
                    intent.putExtra("DATA", mObject.getJSONObject("result").toString())
                    intent.putExtra("MEETING_ID", viewBinding.etMeetingId.text.toString())
                    startActivity(intent)
                }

                override fun onError(error: String) {
                    DialogUtils.cancelDialog(this@JoinMeetingActivity)
                    Utils.showToast(error)
                }
            })
        }
        val finalJsonObject = jsonObject
        viewBinding.btnJoin.setOnLongClickListener {
            viewBinding.etMeetingPassword.setText(finalJsonObject.optString("presenterPasscode"))
            true
        }
    }
}