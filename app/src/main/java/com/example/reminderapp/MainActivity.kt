package com.example.reminderapp

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var etLabel: EditText
    private lateinit var rgRepeat: RadioGroup
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button
    private lateinit var reminderList: LinearLayout
    private lateinit var repo: ReminderRepository
    private lateinit var alarmManager: AlarmManager
    private var editingId: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker = findViewById(R.id.timePicker)
        etLabel = findViewById(R.id.etLabel)
        rgRepeat = findViewById(R.id.rgRepeat)
        btnAdd = findViewById(R.id.btnAdd)
        btnCancel = findViewById(R.id.btnCancel)
        reminderList = findViewById(R.id.reminderList)
        repo = ReminderRepository(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        requestNeededPermissions()
        refreshList()

        btnAdd.setOnClickListener { saveReminder() }
        btnCancel.setOnClickListener { cancelEdit() }
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isNotEmpty()) {
            requestPermissionLauncher.launch(perms.toTypedArray())
        }
    }

    private fun ensureExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "请允许\"精确闹钟\"权限后再添加提醒",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return false
            }
        }
        return true
    }

    private fun saveReminder() {
        if (!ensureExactAlarmPermission()) return

        val editing = editingId != null
        val hour = timePicker.hour
        val minute = timePicker.minute
        val label = etLabel.text.toString().trim()
        val repeat = rgRepeat.checkedRadioButtonId == R.id.rbDaily
        val id = editingId ?: repo.nextId()
        val reminder = Reminder(id, hour, minute, label, repeat)

        if (editing) {
            AlarmScheduler.cancel(this, alarmManager, id)
            repo.update(reminder)
        } else {
            repo.add(reminder)
        }
        AlarmScheduler.schedule(this, alarmManager, reminder)

        etLabel.text.clear()
        exitEditMode()
        refreshList()
        val msg = if (editing) "已更新提醒" else "已添加提醒 ${formatTime(hour, minute)}"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun enterEditMode(reminder: Reminder) {
        editingId = reminder.id
        timePicker.hour = reminder.hour
        timePicker.minute = reminder.minute
        etLabel.setText(reminder.label)
        rgRepeat.check(if (reminder.repeat) R.id.rbDaily else R.id.rbOnce)
        btnAdd.text = "保存修改"
        btnCancel.visibility = View.VISIBLE
    }

    private fun exitEditMode() {
        editingId = null
        btnAdd.text = "添加提醒"
        btnCancel.visibility = View.GONE
    }

    private fun cancelEdit() {
        exitEditMode()
        etLabel.text.clear()
        rgRepeat.check(R.id.rbDaily)
    }

    private fun deleteReminder(reminder: Reminder) {
        if (editingId == reminder.id) {
            exitEditMode()
            etLabel.text.clear()
            rgRepeat.check(R.id.rbDaily)
        }
        AlarmScheduler.cancel(this, alarmManager, reminder.id)
        repo.remove(reminder.id)
        refreshList()
    }

    private fun refreshList() {
        reminderList.removeAllViews()
        val list = repo.getAll().sortedWith(compareBy({ it.hour }, { it.minute }))
        if (list.isEmpty()) {
            val tv = TextView(this).apply {
                text = "暂无提醒"
                setPadding(0, 8, 0, 8)
            }
            reminderList.addView(tv)
            return
        }
        list.forEach { reminder ->
            val row = layoutInflater.inflate(R.layout.item_reminder, reminderList, false)
            val tvInfo = row.findViewById<TextView>(R.id.tvInfo)
            val btnDelete = row.findViewById<Button>(R.id.btnDelete)
            tvInfo.text =
                "${formatTime(reminder.hour, reminder.minute)}   ${reminder.label.ifBlank { "(无内容)" }}  ·  ${if (reminder.repeat) "每天" else "单次"}"
            row.findViewById<Button>(R.id.btnEdit).setOnClickListener { enterEditMode(reminder) }
            btnDelete.setOnClickListener { deleteReminder(reminder) }
            reminderList.addView(row)
        }
    }

    private fun formatTime(h: Int, m: Int): String = "%02d:%02d".format(h, m)
}
