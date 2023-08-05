package com.expeknow.waifubrowser.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.appcompat.widget.SwitchCompat
import com.expeknow.waifubrowser.R

class OptionsActivity : AppCompatActivity() {
    private var serverSwith : SwitchCompat? = null
    private var nsfwSwitch : SwitchCompat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsfwSwitch = findViewById(R.id.nsfw_switch)
        setContentView(R.layout.activity_options)
        nsfwSwitch?.isChecked = true

        serverSwith = findViewById(R.id.server_switch)
        serverSwith?.isChecked = false
    }
}