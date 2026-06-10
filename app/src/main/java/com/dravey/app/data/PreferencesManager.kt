package com.dravey.app.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dravey_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val templateAdapter = moshi.adapter(PositionTemplate::class.java)

    var voiceLanguage: String
        get() = prefs.getString("voice_lang", "uk-UA") ?: "uk-UA"
        set(value) = prefs.edit().putString("voice_lang", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var positionTemplate: PositionTemplate
        get() {
            val json = prefs.getString("position_template", null)
            return if (json != null) {
                try { templateAdapter.fromJson(json) ?: PositionTemplate() }
                catch (e: Exception) { PositionTemplate() }
            } else PositionTemplate()
        }
        set(value) {
            val json = templateAdapter.toJson(value)
            prefs.edit().putString("position_template", json).apply()
        }
}
