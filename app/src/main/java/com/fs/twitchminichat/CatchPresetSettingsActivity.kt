package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CatchPresetSettingsActivity : AppCompatActivity(R.layout.activity_catch_preset_settings) {

    private lateinit var presetContainer: LinearLayout
    private lateinit var btnSavePresets: Button

    private val rowBindings = mutableListOf<PresetRowBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presetContainer = findViewById(R.id.presetContainer)
        btnSavePresets = findViewById(R.id.btnSavePresets)

        buildRows()

        btnSavePresets.setOnClickListener {
            savePresets()
        }
    }

    private fun buildRows() {
        val presets = loadEditablePresets()

        presetContainer.removeAllViews()
        rowBindings.clear()

        presets.forEachIndexed { index, preset ->
            val section = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(this).apply {
                text = "Preset ${index + 1}"
                textSize = 16f
            }

            val enabled = CheckBox(this).apply {
                text = "Enabled"
                isChecked = preset.enabled
            }

            val label = EditText(this).apply {
                hint = "Label"
                setText(preset.label)
                maxLines = 1
                inputType = InputType.TYPE_CLASS_TEXT
            }

            val command = EditText(this).apply {
                hint = "Command"
                setText(preset.command)
                maxLines = 1
                inputType = InputType.TYPE_CLASS_TEXT
            }

            section.addView(title)
            section.addView(enabled)
            section.addView(label)
            section.addView(command)

            presetContainer.addView(section)

            rowBindings += PresetRowBinding(
                id = preset.id,
                enabled = enabled,
                label = label,
                command = command
            )
        }
    }

    private fun savePresets() {
        val presets = rowBindings.mapIndexed { index, row ->
            CatchPreset(
                id = row.id.ifBlank { "custom_$index" },
                label = row.label.text?.toString().orEmpty().trim().ifBlank { "Preset ${index + 1}" },
                command = row.command.text?.toString().orEmpty().trim(),
                enabled = row.enabled.isChecked
            )
        }

        CatchPresetStore.saveAll(this, presets)

        Toast.makeText(this, "Catch presets saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadEditablePresets(): List<CatchPreset> {
        val current = CatchPresetStore.loadAll(this).toMutableList()

        val defaults = listOf(
            CatchPreset("normal", "Catch", "!pokecatch", true),
            CatchPreset("great", "Great", "!pokecatch great ball", true),
            CatchPreset("ultra", "Ultra", "!pokecatch ultra ball", true),
            CatchPreset("timer", "Timer", "!pokecatch timer ball", true),
            CatchPreset("quick", "Quick", "!pokecatch quick ball", true),
            CatchPreset("repeat", "Repeat", "!pokecatch repeat ball", true)
        )

        while (current.size < 6) {
            current += defaults[current.size]
        }

        return current.take(6)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class PresetRowBinding(
        val id: String,
        val enabled: CheckBox,
        val label: EditText,
        val command: EditText
    )

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CatchPresetSettingsActivity::class.java))
        }
    }
}