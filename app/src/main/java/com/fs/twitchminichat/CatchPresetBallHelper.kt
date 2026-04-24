package com.fs.twitchminichat

object CatchPresetBallHelper {

    fun effectiveBallId(preset: CatchPreset): String? {
        val explicit = preset.ballId?.trim()?.lowercase()
        if (!explicit.isNullOrBlank()) return explicit

        return inferBallIdFromText(
            label = preset.label,
            command = preset.command
        )
    }

    fun isFriendBallPreset(preset: CatchPreset): Boolean {
        return effectiveBallId(preset) == "friend_ball"
    }

    fun canBuyFromPreset(preset: CatchPreset): Boolean {
        return when (effectiveBallId(preset)) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball",
            "great_ball",
            "ultra_ball" -> true
            else -> false
        }
    }

    fun resolveShopBallNameForPreset(preset: CatchPreset): String? {
        return when (effectiveBallId(preset)) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke ball"
            "great_ball" -> "great ball"
            "ultra_ball" -> "ultra ball"
            else -> null
        }
    }

    fun resolveBoughtBallIdForPreset(preset: CatchPreset): String? {
        return when (effectiveBallId(preset)) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke_ball"
            "great_ball" -> "great_ball"
            "ultra_ball" -> "ultra_ball"
            else -> null
        }
    }

    private fun inferBallIdFromText(label: String, command: String): String? {
        val l = normalizeBallText(label)
        val c = normalizeBallText(command)
        val joined = "$l | $c"

        if (c.contains("pokecatch")) return CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC

        return when {
            containsAny(joined, "poke ball", "pokeball") -> "poke_ball"
            containsAny(joined, "great ball", "greatball") -> "great_ball"
            containsAny(joined, "ultra ball", "ultraball") -> "ultra_ball"
            containsAny(joined, "master ball", "masterball") -> "master_ball"
            containsAny(joined, "premier ball", "premierball") -> "premier_ball"

            containsAny(joined, "cherish ball", "cherishball") -> "cherish_ball"
            containsAny(joined, "great cherish ball", "greatcherishball") -> "great_cherish_ball"
            containsAny(joined, "ultra cherish ball", "ultracherishball") -> "ultra_cherish_ball"

            containsAny(joined, "heavy ball", "heavyball") -> "heavy_ball"
            containsAny(joined, "feather ball", "featherball") -> "feather_ball"
            containsAny(joined, "timer ball", "timerball") -> "timer_ball"
            containsAny(joined, "quick ball", "quickball") -> "quick_ball"
            containsAny(joined, "nest ball", "nestball") -> "nest_ball"
            containsAny(joined, "fast ball", "fastball") -> "fast_ball"
            containsAny(joined, "heal ball", "healball") -> "heal_ball"
            containsAny(joined, "repeat ball", "repeatball") -> "repeat_ball"
            containsAny(joined, "friend ball", "friendball") -> "friend_ball"

            containsAny(joined, "frozen ball", "frozenball") -> "frozen_ball"
            containsAny(joined, "night ball", "nightball") -> "night_ball"
            containsAny(joined, "phantom ball", "phantomball") -> "phantom_ball"
            containsAny(joined, "cipher ball", "cipherball") -> "cipher_ball"
            containsAny(joined, "magnet ball", "magnetball") -> "magnet_ball"
            containsAny(joined, "net ball", "netball") -> "net_ball"
            containsAny(joined, "sun ball", "sunball") -> "sun_ball"
            containsAny(joined, "fantasy ball", "fantasyball") -> "fantasy_ball"
            containsAny(joined, "geo ball", "geoball") -> "geo_ball"
            containsAny(joined, "basic ball", "basicball") -> "basic_ball"
            containsAny(joined, "mach ball", "machball") -> "mach_ball"

            containsAny(joined, "luxury ball", "luxuryball") -> "luxury_ball"
            containsAny(joined, "stone ball", "stoneball") -> "stone_ball"
            containsAny(joined, "level ball", "levelball") -> "level_ball"
            containsAny(joined, "clone ball", "cloneball") -> "clone_ball"
            containsAny(joined, "sport ball", "sportball") -> "sport_ball"


            else -> null
        }
    }
    fun shouldHideFromQuickMenu(preset: CatchPreset): Boolean {
        return when (effectiveBallId(preset)) {
            "master_ball",
            "cherish_ball",
            "great_cherish_ball",
            "ultra_cherish_ball" -> true

            else -> false
        }
    }
    private fun containsAny(text: String, vararg needles: String): Boolean {
        return needles.any { needle -> text.contains(needle) }
    }

    private fun normalizeBallText(raw: String): String {
        return raw.trim()
            .lowercase()
            .replace("é", "e")
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("""[^a-z0-9\s!]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }


    fun isCoreStandardPreset(preset: CatchPreset): Boolean {
        return when (effectiveBallId(preset)) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball",
            "great_ball",
            "ultra_ball" -> true
            else -> false
        }
    }
}