package com.fs.twitchminichat

import android.content.Context

object CatchBallReasonFormatter {

    fun format(context: Context, reasonKeys: List<String>): String? {
        if (reasonKeys.isEmpty()) return null

        val labels = reasonKeys.mapNotNull { key ->
            when (key) {
                "base_30" -> context.getString(R.string.ball_reason_base_basic)
                "base_55" -> context.getString(R.string.ball_reason_base_great)
                "base_80" -> context.getString(R.string.ball_reason_base_ultra)

                "buddy_shared_type" -> context.getString(R.string.ball_reason_buddy_shared_type)

                "speed_100_plus" -> context.getString(R.string.ball_reason_speed_100_plus)
                "weight_under_10kg" -> context.getString(R.string.ball_reason_weight_under_10kg)
                "weight_over_100kg" -> context.getString(R.string.ball_reason_weight_over_100kg)
                "hp_100_plus" -> context.getString(R.string.ball_reason_hp_100_plus)
                "evolves_twice" -> context.getString(R.string.ball_reason_evolves_twice)
                "already_caught" -> context.getString(R.string.ball_reason_already_caught)

                "type_ice" -> context.getString(R.string.ball_reason_type_ice)
                "type_dark" -> context.getString(R.string.ball_reason_type_dark)
                "type_ghost" -> context.getString(R.string.ball_reason_type_ghost)
                "type_poison_psychic" -> context.getString(R.string.ball_reason_type_poison_psychic)
                "type_electric_steel" -> context.getString(R.string.ball_reason_type_electric_steel)
                "type_water_bug" -> context.getString(R.string.ball_reason_type_water_bug)
                "type_fire_grass" -> context.getString(R.string.ball_reason_type_fire_grass)
                "type_fairy_dragon" -> context.getString(R.string.ball_reason_type_fairy_dragon)
                "type_rock_ground" -> context.getString(R.string.ball_reason_type_rock_ground)
                "type_normal" -> context.getString(R.string.ball_reason_type_normal)
                "type_fighting_flying" -> context.getString(R.string.ball_reason_type_fighting_flying)
                "quick_window" -> context.getString(R.string.ball_reason_quick_window)
                "timer_window" -> context.getString(R.string.ball_reason_timer_window)

                else -> null
            }
        }.distinct()

        if (labels.isEmpty()) return null
        return labels.joinToString(separator = " • ")
    }
}