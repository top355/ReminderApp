package com.example.reminderapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ReminderRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun getAll(): List<Reminder> {
        val json = prefs.getString("list", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<Reminder>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Reminder(
                    id = o.getInt("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    label = o.optString("label", ""),
                    repeat = o.optBoolean("repeat", true)
                )
            )
        }
        return list
    }

    private fun saveAll(list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("hour", it.hour)
            o.put("minute", it.minute)
            o.put("label", it.label)
            o.put("repeat", it.repeat)
            arr.put(o)
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    fun add(reminder: Reminder) {
        val list = getAll().toMutableList()
        list.add(reminder)
        saveAll(list)
    }

    fun update(reminder: Reminder) {
        saveAll(getAll().map { if (it.id == reminder.id) reminder else it })
    }

    fun remove(id: Int) {
        saveAll(getAll().filter { it.id != id })
    }

    fun nextId(): Int = (getAll().maxOfOrNull { it.id } ?: 0) + 1
}
