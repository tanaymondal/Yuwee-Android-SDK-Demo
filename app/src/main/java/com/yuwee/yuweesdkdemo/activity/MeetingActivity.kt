package com.yuwee.yuweesdkdemo.activity

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuwee.yuweesdkdemo.R
import com.yuwee.yuweesdkdemo.activity.MeetingActivity.ListAdapter.ListHolder
import com.yuwee.yuweesdkdemo.databinding.FragmentHostMeetingBinding
import com.yuwee.yuweesdkdemo.utils.DialogUtils
import com.yuwee.sdk.Yuwee
import com.yuwee.sdk.body.MeetingBody.EditMeetingBody
import com.yuwee.sdk.body.MeetingBody.HostMeetingBody
import com.yuwee.sdk.listener.OnMeetingCallback
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MeetingActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var recyclerView: RecyclerView
    private val arrayList = ArrayList<JSONObject>()
    private val format = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")
    private var dialog: Dialog? = null
    private var selectedDate: Calendar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)
        setUpToolbar()
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ListAdapter()
        val button = findViewById<Button>(R.id.btn_add_meeting)
        button.setOnClickListener { showDialog() }
        getAllMeetings()
    }

    private fun setUpToolbar() {
        if (supportActionBar != null) {
            supportActionBar!!.title = "Meeting"
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        val binding: FragmentHostMeetingBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.fragment_host_meeting, null, false)
        builder.setView(binding.root)
        binding.tvMeetingDate.setOnClickListener(this@MeetingActivity)
        binding.tvHost.setOnClickListener {
            if (binding.etMeetingName.text.toString().trim().isEmpty()) {
                binding.etMeetingName.error = "Please enter meeting name."
                binding.etMeetingName.requestFocus()
                return@setOnClickListener
            }
            if (binding.etMeetingDuration.text.toString().trim().isEmpty()) {
                binding.etMeetingDuration.error = "Please enter meeting duration."
                binding.etMeetingDuration.requestFocus()
                return@setOnClickListener
            }
            if (binding.etAllowedParticipants.text.toString().trim().isEmpty()) {
                binding.etAllowedParticipants.error = "Please enter max allowed participants."
                binding.etAllowedParticipants.requestFocus()
                return@setOnClickListener
            }
            if (binding.tvMeetingDate.text.toString().trim().isEmpty()) {
                binding.tvMeetingDate.error = "Please select date and time."
                binding.tvMeetingDate.requestFocus()
                return@setOnClickListener
            }

            val preList = ArrayList<String>()
            if (binding.rdCallMode.checkedRadioButtonId == R.id.rd_training) {
                val emails = binding.etPresenter.text.toString().trim()

                if (emails.trim().isEmpty()) {
                    binding.etPresenter.error = "Please enter email."
                    return@setOnClickListener
                }
                val email = emails.split(",".toRegex()).toTypedArray()
                if (email.isNotEmpty()) {

                    for (i in email.indices) {
                        if (!Patterns.EMAIL_ADDRESS.matcher(email[i].trim()).matches()) {
                            binding.etPresenter.error = "No. " + (i + 1) + " email is not valid."
                            return@setOnClickListener
                        } else {
                            preList.add(email[i].trim())
                        }
                    }
                }
            }

            val emails = binding.etCallAdmins.text.toString().trim()
            val adminList = ArrayList<String>()
            if (emails.trim().isEmpty()) {
                binding.etCallAdmins.error = "Please enter email."
                return@setOnClickListener
            }
            val email = emails.split(",".toRegex()).toTypedArray()
            if (email.isNotEmpty()) {
                for (i in email.indices) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email[i].trim()).matches()) {
                        binding.etCallAdmins.error = "No. " + (i + 1) + " email is not valid."
                        return@setOnClickListener
                    } else {
                        adminList.add(email[i].trim())
                    }
                }
            }
            hostMeeting(binding.rdCallMode.checkedRadioButtonId == R.id.rd_conference,
                    adminList, preList, binding.etMeetingName.text.toString(),
                    (binding.etMeetingDuration.text.toString().toInt() * 60), binding.etAllowedParticipants.text.toString().toInt())
        }
        binding.rdCallMode.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rd_conference -> {
                    binding.llTraining.visibility = View.GONE
                }
                R.id.rd_training -> {
                    binding.llTraining.visibility = View.VISIBLE
                }
            }
        }
        dialog = builder.create()
        dialog?.show()
    }

    private fun getAllMeetings() {
        DialogUtils.showDialog(this, "Getting all meetings...")
        Yuwee.instance.meetingManager.fetchActiveMeetings(object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                DialogUtils.cancelDialog(this@MeetingActivity)
                arrayList.clear()
                try {
                    for (i in 0 until mObject.getJSONArray("result").length()) {
                        if (!mObject.getJSONArray("result").getJSONObject(i).optBoolean("isDeleted", false)) {
                            arrayList.add(mObject.getJSONArray("result").getJSONObject(i))
                        }
                    }
                    recyclerView.adapter?.notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(error: String) {
                DialogUtils.cancelDialog(this@MeetingActivity)
            }
        })
    }

    private fun hostMeeting(isConf: Boolean, adminList: ArrayList<String>, presenterList: ArrayList<String>, name: String,
                            dateExp: Int, maxAllowed: Int) {
        val date = selectedDate?.time
        if (date != null) {
            DialogUtils.showDialog(this, "Please wait...")
            val body = HostMeetingBody()
            body.callMode = if (isConf) 0 else 1 // 0 = conference, 1 = training
            body.callAdmins = adminList
            if (!isConf) {
                body.presenters = presenterList
            }
            body.maxAllowedParticipant = maxAllowed
            body.meetingStartTime = date.time
            body.meetingExpireDuration = dateExp
            body.meetingName = name
            Yuwee.instance.meetingManager.hostMeeting(body, object : OnMeetingCallback {
                override fun onSuccess(mObject: JSONObject) {
                    DialogUtils.cancelDialog(this@MeetingActivity)
                    dialog?.cancel()
                    getAllMeetings()
                }

                override fun onError(error: String) {
                    DialogUtils.cancelDialog(this@MeetingActivity)
                    Log.e("SDK", error)
                }
            })
        } else {
            Toast.makeText(this, "Invalid date and time selected.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun editMeeting() {
        val adminListToAdd = ArrayList<String>()
        adminListToAdd.add("email@email.com")

        val adminListToRemove = ArrayList<String>()
        adminListToRemove.add("email@email.com")

        val presenterListToAdd = ArrayList<String>()
        presenterListToAdd.add("email@email.com")

        val presenterListToRemove = ArrayList<String>()
        presenterListToRemove.add("email@email.com")

        val body = EditMeetingBody()
        body.addCallAdmin = adminListToAdd
        body.removeCallAdmin = adminListToRemove
        body.addPresenter = presenterListToAdd
        body.removePresenter = presenterListToRemove
        body.callMode = 0 // 0 = conference, 1 = training
        body.isCallAllowedWithoutInitiator = true
        body.maxAllowedParticipant = 30
        body.meetingExpirationTime = Calendar.getInstance().timeInMillis + 3600 // expiry time in milliseconds
        body.meetingName = "Name"
        body.meetingStartTime = Calendar.getInstance().timeInMillis // meeting start time
        body.meetingTokenId = "20470274" // meeting id of which to edit
        Yuwee.instance.meetingManager.editMeeting(body, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                // success
            }

            override fun onError(error: String) {
                // error
            }
        })
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
            val view = LayoutInflater.from(this@MeetingActivity).inflate(R.layout.layout_single_row_meeting, parent, false)
            return ListHolder(view)
        }

        override fun onBindViewHolder(holder: ListHolder, position: Int) {
            try {
                holder.tvName.text = arrayList[position].getString("meetingName")
                holder.tvTime.text = format.format(Date(arrayList[position].getJSONObject("callId").getLong("meetingStartTime")))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun getItemCount(): Int {
            return arrayList.size
        }

        private inner class ListHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val tvName: TextView = itemView.findViewById(R.id.tv_meeting_name)
            val tvTime: TextView = itemView.findViewById(R.id.tv_meeting_time)
            override fun onClick(v: View) {
                when (v.id) {
                    R.id.tv_delete -> deleteMeeting(arrayList[layoutPosition].optString("meetingTokenId"))
                    R.id.tv_join -> {
                        val intent = Intent(this@MeetingActivity, JoinMeetingActivity::class.java)
                        intent.putExtra("DATA", arrayList[layoutPosition].toString())
                        startActivity(intent)
                    }
                    else -> {
                        val builder = AlertDialog.Builder(this@MeetingActivity)
                        builder.setTitle("Meeting Details")
                        builder.setMessage("""
    Name: ${arrayList[layoutPosition].optString("meetingName")}
    Meeting ID: ${arrayList[layoutPosition].optString("meetingTokenId")}
    Start Time: ${format.format(Date(arrayList[layoutPosition].getJSONObject("callId").optLong("meetingStartTime")))}
    Pass Code: ${arrayList[layoutPosition].optString("passcode")}
    Presenter Pass Code: ${arrayList[layoutPosition].optString("presenterPasscode")}
    """.trimIndent())
                        builder.show()
                    }
                }
            }

            init {
                val tvDelete = itemView.findViewById<TextView>(R.id.tv_delete)
                val tvJoin = itemView.findViewById<TextView>(R.id.tv_join)
                itemView.setOnClickListener(this)
                tvDelete.setOnClickListener(this)
                tvJoin.setOnClickListener(this)
            }
        }
    }

    private fun deleteMeeting(meetingTokenId: String) {
        Log.e("SDK", meetingTokenId)
        DialogUtils.showDialog(this, "Deleting meeting...")
        Yuwee.instance.meetingManager.deleteMeeting(meetingTokenId, object : OnMeetingCallback {
            override fun onSuccess(mObject: JSONObject) {
                DialogUtils.cancelDialog(this@MeetingActivity)
                for (i in arrayList.indices) {
                    if (arrayList[i].optString("meetingTokenId").equals(meetingTokenId, ignoreCase = true)) {
                        arrayList.removeAt(i)
                        recyclerView.adapter?.notifyDataSetChanged()
                        break
                    }
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@MeetingActivity, "error", Toast.LENGTH_SHORT).show()
                DialogUtils.cancelDialog(this@MeetingActivity)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_meeting_date -> openDatePicker(v as TextView)
        }
    }

    private fun openDatePicker(view: TextView) {
        view.text = ""
        val datePicker = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedDate = Calendar.getInstance()
            selectedDate?.set(Calendar.YEAR, year)
            selectedDate?.set(Calendar.MONTH, month)
            selectedDate?.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            openTimePicker(view)
        }, Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis
        datePicker.setCancelable(false)
        datePicker.show()

    }

    private fun openTimePicker(textView: TextView) {
        val timePicker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            selectedDate?.set(Calendar.HOUR_OF_DAY, hourOfDay)
            selectedDate?.set(Calendar.MINUTE, minute)
            selectedDate?.set(Calendar.SECOND, 0)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm")

            val date = selectedDate?.time
            textView.text = format.format(date?.time)

        }, Calendar.getInstance().get(Calendar.HOUR),
                Calendar.getInstance().get(Calendar.MINUTE),
                false
        )
        timePicker.setCancelable(false)
        timePicker.show()


    }
}