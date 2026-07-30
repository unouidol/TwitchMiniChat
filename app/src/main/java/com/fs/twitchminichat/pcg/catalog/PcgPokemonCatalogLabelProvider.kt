package com.fs.twitchminichat.pcg.catalog

import android.content.Context
import androidx.annotation.StringRes
import com.fs.twitchminichat.R

/** Converts catalog enum data into localized user-facing labels. */
object PcgPokemonCatalogLabelProvider {

    /** Builds the compact metadata line shown below one catalog name. */
    fun metadata(
        context: Context,
        entry: PcgPokemonCatalogEntry
    ): String {
        val tier = context.getString(tierLabel(entry.tier))
        val types = entry.types.joinToString(
            separator = context.getString(R.string.pcg_type_separator)
        ) { type ->
            context.getString(typeLabel(type))
        }
        val generation = context.getString(
            R.string.pcg_generation_label,
            entry.generation
        )
        val availability = context.getString(
            if (entry.normallySpawnable) {
                R.string.pcg_availability_normal
            } else {
                R.string.pcg_availability_special
            }
        )

        return context.getString(
            R.string.pcg_most_wanted_item_metadata,
            tier,
            types,
            generation,
            availability
        )
    }

    /** Maps one PCG tier to its localized label. */
    @StringRes
    private fun tierLabel(tier: PcgPokemonTier): Int {
        return when (tier) {
            PcgPokemonTier.S -> R.string.pcg_tier_s
            PcgPokemonTier.A -> R.string.pcg_tier_a
            PcgPokemonTier.B -> R.string.pcg_tier_b
            PcgPokemonTier.C -> R.string.pcg_tier_c
        }
    }

    /** Maps one elemental type to its localized label. */
    @StringRes
    private fun typeLabel(type: PcgPokemonType): Int {
        return when (type) {
            PcgPokemonType.NORMAL -> R.string.pcg_type_normal
            PcgPokemonType.FIRE -> R.string.pcg_type_fire
            PcgPokemonType.WATER -> R.string.pcg_type_water
            PcgPokemonType.ELECTRIC -> R.string.pcg_type_electric
            PcgPokemonType.GRASS -> R.string.pcg_type_grass
            PcgPokemonType.ICE -> R.string.pcg_type_ice
            PcgPokemonType.FIGHTING -> R.string.pcg_type_fighting
            PcgPokemonType.POISON -> R.string.pcg_type_poison
            PcgPokemonType.GROUND -> R.string.pcg_type_ground
            PcgPokemonType.FLYING -> R.string.pcg_type_flying
            PcgPokemonType.PSYCHIC -> R.string.pcg_type_psychic
            PcgPokemonType.BUG -> R.string.pcg_type_bug
            PcgPokemonType.ROCK -> R.string.pcg_type_rock
            PcgPokemonType.GHOST -> R.string.pcg_type_ghost
            PcgPokemonType.DRAGON -> R.string.pcg_type_dragon
            PcgPokemonType.DARK -> R.string.pcg_type_dark
            PcgPokemonType.STEEL -> R.string.pcg_type_steel
            PcgPokemonType.FAIRY -> R.string.pcg_type_fairy
        }
    }
}