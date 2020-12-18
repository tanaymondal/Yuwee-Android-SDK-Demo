package com.yuwee.yuweesdkdemo.activity

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.yuwee.yuweesdkdemo.DemoService
import com.yuwee.yuweesdkdemo.R
import com.yuwee.yuweesdkdemo.app.DemoApp
import com.yuwee.yuweesdkdemo.databinding.ActivityMeetingCallBinding
import com.yuwee.yuweesdkdemo.databinding.LayoutSingleRowMemberListBinding
import com.yuwee.yuweesdkdemo.utils.DialogUtils
import com.yuwee.yuweesdkdemo.utils.PrefUtils
import com.yuwee.yuweesdkdemo.utils.Utils
import com.yuwee.sdk.Yuwee
import com.yuwee.sdk.body.MeetingBody
import com.yuwee.sdk.enums.MeetingType
import com.yuwee.sdk.enums.RoleType
import com.yuwee.sdk.ibase.RemoteStream.StreamObserver
import com.yuwee.sdk.ibase.Stream.StreamSourceInfo.VideoSourceInfo
import com.yuwee.sdk.iconference.RemoteStream
import com.yuwee.sdk.listener.*
import com.yuwee.sdk.meet_class.MediaType
import com.yuwee.sdk.meet_class.StreamSubscription
import com.yuwee.sdk.model.MeetingParams
import com.yuwee.sdk.view.YuweeVideoView
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MeetingCallActivity : AppCompatActivity(), StreamObserver, View.OnClickListener, OnHostedMeetingListener {
    private lateinit var jsonObject: JSONObject
    private val streamArrayList = ArrayList<StreamClass>()
    private var isScreenSharingStarted = false
    private lateinit var viewBinding: ActivityMeetingCallBinding

    private val memberList = ArrayList<MemberDetails>()
    private var isPresenter = false
    private var isSubPresenter = false
    private var isAdmin = false
    private var email = PrefUtils.getInstance().userLogin.result.user.email
    private var isHandRaised = false
    private var isAudioEnabled = true
    private var isVideoEnabled = true
    private val meetingParams = MeetingParams()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

/*        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);*/
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_meeting_call)

        setSupportActionBar(viewBinding.toolbar)
        addOngoingNotification()

        getIntentData()
        onClick()
        initMeeting()
        Yuwee.instance.meetingManager.setMeetingListener(this)
        setUpMemberRecyclerView()
        getActiveParticipants()
    }

    private fun getActiveParticipants() {
        Yuwee.instance.meetingManager.fetchActiveParticipantsList(object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                val result = mObject.optJSONArray("result")
                if (result != null) {
                    for (index in 0 until result.length()) {
                        val mObj = result.optJSONObject(index)
                        val details = MemberDetails(
                                mObj.optString("_id"),
                                mObj.optString("name"),
                                mObj.optString("email"),
                                "",
                                "",
                                mObj.optBoolean("isAudioOn"),
                                mObj.optBoolean("isVideoOn"),
                                mObj.optBoolean("isPresenter"),
                                mObj.optBoolean("isSubPresenter"),
                                mObj.optBoolean("isCallAdmin"),
                                false
                        )

                        memberList.add(details)
                    }
                    viewBinding.recyclerUserList.adapter?.notifyDataSetChanged()
                }
            }

            override fun onError(error: String) {

            }

        })
    }

    private fun setUpMemberRecyclerView() {
        viewBinding.recyclerUserList.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerUserList.adapter = MemberListAdapter()
    }

    private fun setUpCameraRecyclerViews() {
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = RecyclerAdapter()
    }

    private fun onClick() {
        viewBinding.incControl.ivEnd.setOnClickListener(this)
        viewBinding.incControl.audio.setOnClickListener(this)
        viewBinding.incControl.video.setOnClickListener(this)
        viewBinding.incControl.ivHandRaise.setOnClickListener(this)
    }

    private fun getIntentData() {
        try {

            jsonObject = JSONObject(intent.extras?.getString("DATA", "") as String)

            meetingParams.meetingTokenId = intent.extras?.getString("MEETING_ID", "") as String
            meetingParams.callId = jsonObject.getJSONObject("callData").optString("callId")
            meetingParams.icsCallResourceId = jsonObject.getJSONObject("callTokenInfo").optString("ICSCallResourceId")
            meetingParams.token = jsonObject.getJSONObject("callTokenInfo").optString("token")
            meetingParams.roomId = jsonObject.getJSONObject("callData").optString("roomId")
            meetingParams.userId = PrefUtils.getInstance().userLogin.result.user.id
            meetingParams.meetingType = if (jsonObject.getJSONObject("callData").optInt("callMode", 0) == 0) MeetingType.CONFERENCE else MeetingType.TRAINING

            if (meetingParams.meetingType == MeetingType.TRAINING) {
                viewBinding.llTraining.visibility = View.VISIBLE
                setUpCameraRecyclerViews()
                viewBinding.yvConfRemoteMixedStream.visibility = View.GONE
                viewBinding.yvConfRemoteScreenStream.visibility = View.GONE
            }

            val arrayAdmins: JSONArray? = jsonObject.optJSONObject("callData")?.optJSONArray("callAdmins")
            if (arrayAdmins != null) {
                for (i in 0 until arrayAdmins.length()) {
                    if (arrayAdmins.getString(i).equals(email, ignoreCase = true)) {
                        isAdmin = true
                        break
                    }
                }
            }

            when (jsonObject.optString("role", "")) {
                "presenter" -> isPresenter = true
                "subPresenter" -> isSubPresenter = true
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initMeeting() {
        DialogUtils.showDialog(this, "Joining meeting...")
        Yuwee.instance.meetingManager.joinMeeting(meetingParams, object : OnInitMeetingListener {
            override fun onJoinMeetingSuccessful() {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
                getAllStreams()
                if (meetingParams.meetingType == MeetingType.TRAINING) {
                    if (isPresenter) {
                        publishCameraStream(true)
                    } else if (isSubPresenter) {
                        publishCameraStream(false)
                    }
                } else {
                    publishCameraStream(true) // all are presenter in conference mode
                }
            }

            override fun onJoinMeetingError(error: String) {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
            }
        })
    }

    private fun getAllStreams() {
        val streamList = Yuwee.instance.meetingManager.getAllRemoteStream()
        for (item in streamList) {
            val streamClass = StreamClass()
            streamClass.stream = item
            streamArrayList.add(streamClass)

            if (meetingParams.meetingType == MeetingType.CONFERENCE) {
                if (item.streamSourceInfo.videoSourceInfo == VideoSourceInfo.MIXED) {
                    subscribeStream(item, viewBinding.yvConfRemoteMixedStream)
                } else if (item.streamSourceInfo.videoSourceInfo == VideoSourceInfo.SCREEN_CAST) {
                    subscribeStream(item, viewBinding.yvConfRemoteScreenStream)
                }
            } else {
                streamAddedOnTraining()
            }
        }
    }

    private fun streamAddedOnTraining() {
        viewBinding.recyclerView.adapter?.notifyItemInserted(streamArrayList.size - 1)
    }

    private fun publishCameraStream(isPresenter: Boolean) {
        DialogUtils.showDialog(this, "Publishing camera stream...")
        Yuwee.instance.meetingManager.publishCameraStream(if (isPresenter) RoleType.PRESENTER else RoleType.SUB_PRESENTER, object : OnPublishStreamListener {
            override fun onPublishStreamSuccessful() {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
            }

            override fun onPublishStreamError(error: String) {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
            }
        })
    }

    private fun subscribeStream(stream: RemoteStream, videoView: YuweeVideoView) {
        Log.e("TANAY", "subscribeStream")
        stream.addObserver(this@MeetingCallActivity)
        Log.e("TANAY", Gson().toJson(stream.publicationSettings.videoPublicationSettings))
        Yuwee.instance.meetingManager.subscribeRemoteStream(videoView, stream, object : OnSubscribeStreamListener {
            override fun onSubscribeStreamSuccessful(subscription: StreamSubscription) {
                Log.e("TANAY", "onSubscribeStreamSuccessful")
                Log.e("TANAY", Gson().toJson(stream.publicationSettings.videoPublicationSettings))
                for (item in streamArrayList) {
                    if (item.stream?.id() == subscription.getRemoteStreamId()) {
                        item.subscription = subscription
                        break
                    }
                }
                if (meetingParams.meetingType == MeetingType.CONFERENCE) {
                    if (stream.streamSourceInfo.videoSourceInfo == VideoSourceInfo.SCREEN_CAST) {
                        viewBinding.yvConfRemoteMixedStream.visibility = View.GONE
                        viewBinding.yvConfRemoteScreenStream.visibility = View.VISIBLE
                    }
                }
            }

            override fun onSubscribeStreamError(error: String) {
                Utils.showToast("subscribe error")
            }
        })
    }

    override fun onEnded(stream: com.yuwee.sdk.ibase.RemoteStream) {
        runOnUiThread {
            for (i in streamArrayList.indices) {
                if (streamArrayList[i].stream?.id().equals(stream.id(), ignoreCase = true)) {
                    if (meetingParams.meetingType == MeetingType.CONFERENCE) {
                        if (stream.streamSourceInfo.videoSourceInfo == VideoSourceInfo.SCREEN_CAST) {
                            viewBinding.yvConfRemoteMixedStream.visibility = View.VISIBLE
                            viewBinding.yvConfRemoteScreenStream.visibility = View.GONE
                            Utils.showToast("Screen sharing stopped.")
                        }
                    } else {
                        streamArrayList.removeAt(i)
                        viewBinding.recyclerView.adapter?.notifyItemRemoved(i)
                        break
                    }
                }
            }
        }
    }

    override fun onUpdated(stream: com.yuwee.sdk.ibase.RemoteStream) {}
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.meeting_call, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.getItem(1).title = if (isScreenSharingStarted) "Stop Screen Sharing" else "Start Screen Sharing"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_switch_camera -> Yuwee.instance.meetingManager.switchCamera()
            R.id.menu_start_screen_sharing -> if (isScreenSharingStarted) {
                Yuwee.instance.meetingManager.unpublishScreenStream()
                viewBinding.tvScreenText.visibility = View.GONE
                isScreenSharingStarted = false
            } else {
                askScreenSharingPermission()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun askScreenSharingPermission() {
        val manager: MediaProjectionManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(manager.createScreenCaptureIntent(), SCREEN_SHARING_REQUEST_CODE)
        } else {
            Utils.showToast("Screen sharing is not supported.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_SHARING_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Utils.showToast("Please give permission to capture screen.")
                return
            }
            if (data != null) {
                startScreenSharing(data)
            } else {
                Utils.showToast("Error sharing screen.")
            }
        }
    }

    private fun startScreenSharing(data: Intent) {
        // TODO: 13/8/20 change roleType
        Yuwee.instance.meetingManager.publishScreenStream(data, RoleType.PRESENTER, object : OnPublishStreamListener {
            override fun onPublishStreamSuccessful() {
                isScreenSharingStarted = true
                viewBinding.tvScreenText.visibility = View.VISIBLE
            }

            override fun onPublishStreamError(error: String) {}
        })
    }

    private fun leaveMeeting() {
        DialogUtils.showDialog(this, "Leaving...")
        Yuwee.instance.meetingManager.leaveMeeting(object : OnLeaveMeetingListener {
            override fun onLeaveMeetingSuccessful() {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
                finish()
            }

            override fun onLeaveMeetingError(error: String) {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
                finish()
            }
        })
    }

    private fun endMeeting() {
        DialogUtils.showDialog(this, "Ending...")
        Yuwee.instance.meetingManager.endMeeting(object : OnEndMeetingListener {
            override fun onEndMeetingSuccessful() {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
                finish()
            }

            override fun onEndMeetingError(error: String) {
                DialogUtils.cancelDialog(this@MeetingCallActivity)
                finish()
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_end -> leaveMeeting()
            R.id.iv_hand_raise -> raiseHand()
/*            R.id.iv_chat -> openChatActivity()
            R.id.iv_menu -> viewBinding.drawerLayout.openDrawer(GravityCompat.START)
            R.id.iv_three_dot -> showPopUpMenu()*/
            R.id.audio -> {
                isAudioEnabled = !isAudioEnabled
                Yuwee.instance.meetingManager.setMediaEnabled(isAudioEnabled, isVideoEnabled)
            }
            R.id.video -> {
                isVideoEnabled = !isVideoEnabled
                Yuwee.instance.meetingManager.setMediaEnabled(isAudioEnabled, isVideoEnabled)
            }
        }
    }

    private fun addOngoingNotification() {
        val `in` = Intent(DemoApp.getInstance(), DemoService::class.java)
        `in`.action = SHOW_ONGOING_CALL_NOTIFICATION
        ContextCompat.startForegroundService(DemoApp.getInstance(), `in`)
    }

    private fun stopOngoingNotification() {
        val `in` = Intent(DemoApp.getInstance(), DemoService::class.java)
        `in`.action = STOP_ONGOING_CALL_NOTIFICATION
        ContextCompat.startForegroundService(DemoApp.getInstance(), `in`)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopOngoingNotification()
    }

    private fun showPublishDialog(isPresenter: Boolean) {
        AlertDialog.Builder(this).apply {
            setTitle("Alert!")
            setMessage(if (isPresenter) "You are now a Presenter! Do you want to publish your video?"
            else "You are now a Sub-Presenter! Do you want to publish your video?")
            setCancelable(false)
            setPositiveButton("Yes") { _, _ -> publishCameraStream(isPresenter) }
            setNegativeButton("No") { _, _ -> }
            show()
        }
    }

    //region Listener methods
    override fun onStreamAdded(remoteStream: RemoteStream) {
        Log.e("TANAY", "onStreamAdded")
        if (meetingParams.meetingType == MeetingType.CONFERENCE) {
            if (remoteStream.streamSourceInfo.videoSourceInfo == VideoSourceInfo.SCREEN_CAST) {
                subscribeStream(remoteStream, viewBinding.yvConfRemoteScreenStream)
            }
        } else {
            val streamClass = StreamClass()
            streamClass.stream = remoteStream
            streamArrayList.add(streamClass)
            streamAddedOnTraining()
        }
    }

    override fun onCallPresentersUpdated(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }

        if (userId.equals(meetingParams.userId, ignoreCase = true)) {
            when (data.optString("newRole", "")) {
                "presenter" -> {
                    isPresenter = true
                    isSubPresenter = false
                }
                "subPresenter" -> {
                    isPresenter = false
                    isSubPresenter = true
                }
                else -> {
                    isPresenter = false
                    isSubPresenter = false
                }
            }
            viewBinding.recyclerUserList.adapter?.notifyDataSetChanged()
            when {
                isPresenter -> {
                    Yuwee.instance.meetingManager.unpublishCameraStream()
                    showPublishDialog(isPresenter = true)
                }
                isSubPresenter -> {
                    Yuwee.instance.meetingManager.unpublishCameraStream()
                    showPublishDialog(isPresenter = false)
                }
                else -> {
                    Yuwee.instance.meetingManager.unpublishCameraStream()
                }
            }
        }

        for (mData in memberList) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                mData.isPresenter = data.optBoolean("isPresenter", false)
                break
            }
        }

    }

    override fun onCallAdminsUpdated(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }

        if (userId.equals(meetingParams.userId, ignoreCase = true)) {
            isAdmin = data.optBoolean("isCallAdmin", false)
            viewBinding.recyclerUserList.adapter?.notifyDataSetChanged()
        }

        for (mData in memberList) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                mData.isAdmin = data.optBoolean("isCallAdmin", false)
                viewBinding.recyclerUserList.adapter?.notifyDataSetChanged()
                break
            }
        }

    }

    override fun onCallParticipantMuted(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }
        for ((index, mData) in memberList.withIndex()) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                mData.isAudioOn = !data.optBoolean("isMuted", false)
                viewBinding.recyclerUserList.adapter?.notifyItemChanged(index)
                break
            }
        }

    }

    override fun onCallParticipantDropped(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }
        for (mData in memberList) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                Utils.showToast("You are removed by Admin")
                leaveMeeting()
                break
            }
        }

    }

    override fun onCallParticipantJoined(data: JSONObject) {
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }
        val mObj = data.optJSONObject("info")
        if (mObj != null) {
            val mData = MemberDetails(
                    mObj.optString("_id"),
                    mObj.optString("name"),
                    mObj.optString("email"),
                    "",
                    "",
                    mObj.optBoolean("isAudioOn"),
                    mObj.optBoolean("isVideoOn"),
                    mObj.optBoolean("isPresenter"),
                    data.optBoolean("isSubPresenter"),
                    mObj.optBoolean("isCallAdmin"),
                    false
            )
            memberList.add(mData)
            viewBinding.recyclerUserList.adapter?.notifyItemInserted(memberList.size - 1)
        }
    }

    override fun onCallParticipantLeft(data: JSONObject) {
        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }
        for ((index, mData) in memberList.withIndex()) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                memberList.removeAt(index)
                viewBinding.recyclerUserList.adapter?.notifyItemRemoved(index)
                break
            }
        }
    }

    override fun onCallParticipantStatusUpdated(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }

        for ((index, mData) in memberList.withIndex()) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                if (data.getJSONObject("info").has("isAudioOn")) {
                    mData.isAudioOn = data.getJSONObject("info").optBoolean("isAudioOn", false)
                }
                if (data.getJSONObject("info").has("isVideoOn")) {
                    mData.isVideoOn = data.getJSONObject("info").optBoolean("isVideoOn", false)
                }
                viewBinding.recyclerUserList.adapter?.notifyItemChanged(index)
                break
            }
        }

    }

    override fun onCallHandRaised(data: JSONObject) {

        val userId = data.optString("userId", "")
        val callId = data.optString("callId", "")
        if (meetingParams.callId != callId) {
            return
        }
        for ((index, mData) in memberList.withIndex()) {
            if (mData._id.equals(userId, ignoreCase = true)) {
                mData.isHandRaised = data.optBoolean("isHandRaised", false)
                viewBinding.recyclerUserList.adapter?.notifyItemChanged(index)
                break
            }
        }

    }

    override fun onCallActiveSpeaker(data: JSONObject) {
       // Log.e("TANAY", "onCallActiveSpeaker $data")
    }

    override fun onMeetingEnded(data: JSONObject) {
        leaveMeeting()
    }

    override fun onError(error: String) {
        Log.e("SDK", error)
    }
    //endregion

    //region API Calls

    private fun toggleAdminStatus(position: Int) {
        val body = MeetingBody.CallAdminBody()
        body.isCallAdmin = !memberList[position].isAdmin
        body.userId = memberList[position]._id

        Yuwee.instance.meetingManager.makeOrRevokeAdmin(body, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                if (mObject.getString("status").equals("success", ignoreCase = true)) {
                    Utils.showToast(mObject.getString("message"))
                    memberList[position].isAdmin = !memberList[position].isAdmin
                    viewBinding.recyclerUserList.adapter?.notifyItemChanged(position)
                } else {
                    Utils.showToast(mObject.getString("message"))
                }
            }

            override fun onError(error: String) {
                Utils.showToast("toggleAdminStatus error")
            }

        })
    }

    private fun togglePresenterStatus(position: Int, roleType: RoleType) {
        val body = MeetingBody.CallPresenterBody()
        memberList[position].isPresenter = !memberList[position].isPresenter
        body.isTempPresenter = false
        body.userId = memberList[position]._id

        Yuwee.instance.meetingManager.updatePresenterStatus(body, roleType, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {

                if (mObject.getString("status").equals("success", ignoreCase = true)) {
                    Utils.showToast(mObject.getString("message"))
                    memberList[position].isPresenter = !memberList[position].isPresenter
                    viewBinding.recyclerUserList.adapter?.notifyItemChanged(position)
                } else {
                    Utils.showToast(mObject.getString("message"))
                }
            }

            override fun onError(error: String) {
                Utils.showToast("togglePresenterStatus error")
            }

        })
    }

    private fun toggleMuteUnmuteStatus(position: Int) {
        val body = MeetingBody.MuteUnmuteBody()
        body.audioStatus = !memberList[position].isAudioOn
        body.userId = memberList[position]._id
        Yuwee.instance.meetingManager.toggleParticipantAudio(body, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                if (mObject.getString("status").equals("success", ignoreCase = true)) {
                    Utils.showToast(mObject.getString("message"))
                    memberList[position].isAudioOn = !memberList[position].isAudioOn
                    viewBinding.recyclerUserList.adapter?.notifyItemChanged(position)
                } else {
                    Utils.showToast(mObject.getString("message"))
                }
            }

            override fun onError(error: String) {
                Utils.showToast("toggleMuteUnmuteStatus error")
            }

        })
    }

    private fun dropMember(position: Int) {
        if (meetingParams.userId.equals(memberList[position]._id, ignoreCase = true)) {
            Utils.showToast("You can't drop yourself.")
            return
        }

        val body = MeetingBody.DropParticipantBody()
        body.userId = memberList[position]._id

        Yuwee.instance.meetingManager.dropParticipant(body, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                if (mObject.getString("status").equals("success", ignoreCase = true)) {
                    Utils.showToast(mObject.getString("message"))
                    memberList.removeAt(position)
                    viewBinding.recyclerUserList.adapter?.notifyItemRemoved(position)
                } else {
                    Utils.showToast(mObject.getString("message"))
                }
            }

            override fun onError(error: String) {
                Utils.showToast("dropMember error")
            }

        })
    }

    private fun raiseHand() {

        val body = MeetingBody.HandRaiseBody()
        body.raiseHand = !isHandRaised
        body.userId = PrefUtils.getInstance().userLogin.result.user.id

        Yuwee.instance.meetingManager.toggleHandRaise(body, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                if (mObject.getString("status").equals("success", ignoreCase = true)) {
                    Utils.showToast(mObject.getString("message"))
                    isHandRaised = !isHandRaised
                    viewBinding.incControl.ivHandRaise.setColorFilter(
                            ContextCompat.getColor(this@MeetingCallActivity,
                                    if (isHandRaised) R.color.yuwee_accent_color else R.color.white),
                            android.graphics.PorterDuff.Mode.MULTIPLY)
                    for ((index, data) in memberList.withIndex()) {
                        if (data._id.equals(meetingParams.userId, ignoreCase = true)) {
                            data.isHandRaised = isHandRaised
                            viewBinding.recyclerUserList.adapter?.notifyItemChanged(index)
                            break
                        }
                    }
                } else {
                    Utils.showToast(mObject.getString("message"))
                }
            }

            override fun onError(error: String) {
                Utils.showToast("dropMember error")
            }

        })
    }
    //endregion

    private inner class RecyclerAdapter : RecyclerView.Adapter<RecyclerAdapter.Holder>() {

        override fun onViewAttachedToWindow(holder: Holder) {
            super.onViewAttachedToWindow(holder)
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                val stream = streamArrayList[holder.adapterPosition].stream
                if (streamArrayList[holder.adapterPosition].subscription != null && stream != null) {
                    streamArrayList[holder.adapterPosition].subscription?.unmute(MediaType.VIDEO)
                    Yuwee.instance.meetingManager.attachRemoteStream(holder.surfaceView, stream)
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            super.onViewDetachedFromWindow(holder)
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                val stream = streamArrayList[holder.adapterPosition].stream
                if (streamArrayList[holder.adapterPosition].subscription != null && stream != null) {
                    streamArrayList[holder.adapterPosition].subscription?.mute(MediaType.VIDEO)
                    Yuwee.instance.meetingManager.detachRemoteStream(holder.surfaceView, stream)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.Holder {
            val view = LayoutInflater.from(this@MeetingCallActivity).inflate(R.layout.layout_single_row_call, parent, false)

            /*     view.layoutParams.height = AppData.getScreenHeight() / 2
                 view.layoutParams.width = AppData.getScreenWidth() / 2*/

            return Holder(view)
        }

        override fun getItemCount(): Int {
            return streamArrayList.size
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val stream = streamArrayList[position].stream
            if (stream != null) {
                if (streamArrayList[position].subscription != null) { // means the stream is already subscribed, no need to subscribe again
                    Yuwee.instance.meetingManager.attachRemoteStream(holder.surfaceView, stream)
                } else {
                    subscribeStream(stream, holder.surfaceView)
                }
            }

        }

        private inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val surfaceView: YuweeVideoView = itemView.findViewById(R.id.camera_renderer)
        }
    }

    private inner class MemberListAdapter : RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            val binding: LayoutSingleRowMemberListBinding = DataBindingUtil.inflate(LayoutInflater.from(this@MeetingCallActivity), R.layout.layout_single_row_member_list, parent, false)
            return MemberViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            holder.binding.tvEmail.text = memberList[position].email
            holder.binding.tvName.text = memberList[position].name
            holder.binding.cbAudio.isChecked = !memberList[position].isAudioOn
            holder.binding.cbVideo.isChecked = !memberList[position].isVideoOn
            if (memberList[position]._id.equals(meetingParams.userId, ignoreCase = true)) {
                holder.binding.ivThreeDot.visibility = View.INVISIBLE
            } else {
                holder.binding.ivThreeDot.visibility = if (isAdmin) View.VISIBLE else View.INVISIBLE
            }
            holder.binding.ivHand.visibility = if (memberList[position].isHandRaised) View.VISIBLE else View.GONE

            val options = RequestOptions()
                    .circleCrop()
                    .placeholder(ContextCompat.getDrawable(this@MeetingCallActivity, R.drawable.profile_icon_new))
                    .error(ContextCompat.getDrawable(this@MeetingCallActivity, R.drawable.profile_icon_new))
                    .priority(Priority.HIGH)

            Glide.with(this@MeetingCallActivity)
                    .load(memberList[position].image)
                    .apply(options)
                    .into(holder.binding.ivUser)
        }

        private inner class MemberViewHolder(val binding: LayoutSingleRowMemberListBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

            init {
                binding.ivThreeDot.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                when (v?.id) {
                    R.id.iv_three_dot -> {
                        val menu = PopupMenu(this@MeetingCallActivity, binding.ivThreeDot)
                        menu.menu.add(if (memberList[layoutPosition].isAdmin) REMOVE_ADMIN else MAKE_ADMIN)
                        if (meetingParams.meetingType == MeetingType.TRAINING) {
                            menu.menu.add(if (memberList[layoutPosition].isPresenter) REMOVE_PRESENTER else MAKE_PRESENTER)
                        }
                        menu.menu.add(if (memberList[layoutPosition].isAudioOn) MUTE else UNMUTE)
                        menu.menu.add(MAKE_SUB_PRESENTER)
                        menu.menu.add(DROP)
                        menu.setOnMenuItemClickListener { item ->
                            when (item?.title.toString()) {
                                REMOVE_ADMIN -> toggleAdminStatus(layoutPosition)
                                MAKE_ADMIN -> toggleAdminStatus(layoutPosition)
                                REMOVE_PRESENTER -> togglePresenterStatus(layoutPosition, RoleType.VIEWER)
                                MAKE_PRESENTER -> togglePresenterStatus(layoutPosition, RoleType.PRESENTER)
                                MAKE_SUB_PRESENTER -> togglePresenterStatus(layoutPosition, RoleType.SUB_PRESENTER)
                                UNMUTE -> toggleMuteUnmuteStatus(layoutPosition)
                                MUTE -> toggleMuteUnmuteStatus(layoutPosition)
                                DROP -> dropMember(layoutPosition)
                            }
                            true
                        }
                        menu.show()
                    }
                }
            }


        }

    }

    private data class MemberDetails(
            var _id: String,
            var name: String,
            var email: String,
            var image: String,
            var nickName: String,
            var isAudioOn: Boolean,
            var isVideoOn: Boolean,
            var isPresenter: Boolean,
            var isSubPresenter: Boolean,
            var isAdmin: Boolean,
            var isHandRaised: Boolean
    )

    companion object {
        private const val SCREEN_SHARING_REQUEST_CODE = 11003
        const val SHOW_ONGOING_CALL_NOTIFICATION = "call_notification"
        const val STOP_ONGOING_CALL_NOTIFICATION = "stop_ongoing"
        private const val REMOVE_ADMIN = "Remove Admin"
        private const val MAKE_ADMIN = "Make Admin"
        private const val REMOVE_PRESENTER = "Remove Presenter"
        private const val MAKE_PRESENTER = "Make Presenter"
        private const val MUTE = "Mute"
        private const val UNMUTE = "Unmute"
        private const val DROP = "Drop"
        private const val MAKE_SUB_PRESENTER = "Make Sub-Presenter"

        @JvmStatic
        fun showOngoingCallNotification(): Notification {
            val channelId = "call_notification_channel_id"
            val channelDescription = "Call Notification Channel"
            val builder = NotificationCompat.Builder(DemoApp.getInstance(), channelId)
            builder.setContentTitle("Call")
            builder.setContentText("Call in progress...")
            //builder.setSmallIcon(NotificationUtils.getNotificationIcon());
            builder.setOngoing(true)
            builder.setAutoCancel(false)
            builder.setUsesChronometer(true)
            val notificationManager = DemoApp.getInstance().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, channelDescription, NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            return builder.build()
        }
    }

    private class StreamClass {
        var subscription: StreamSubscription? = null
        var stream: RemoteStream? = null
    }
}