package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import java.util.Locale

/**
 * Canonical Smart Catch catalog backed by the complete PCG inventory.
 *
 * Exact PCG names always outrank aliases. An alias is indexed only when every
 * occurrence belongs to the same canonical entry, preventing load order from
 * silently changing one form into another.
 */
internal class PokemonTypeDexCatalog private constructor(
    val entries: List<PokemonTypeEntry>,
    private val byCanonicalName: Map<String, PokemonTypeEntry>,
    private val byUniqueAlias: Map<String, PokemonTypeEntry>
) {

    /** Resolves canonical PCG names first and then safe unique aliases. */
    fun find(rawName: String): PokemonTypeEntry? {
        val lookupKey = PcgPokemonNameNormalizer.normalize(rawName)
        if (lookupKey.isBlank()) return null

        return byCanonicalName[lookupKey]
            ?: byUniqueAlias[lookupKey]
    }

    companion object {

        /** Combines the canonical PCG inventory with optional Smart Catch statistics. */
        fun build(
            pcgEntries: List<PcgPokemonCatalogEntry>,
            metadataEntries: List<PokemonTypeDexMetadataEntry>
        ): PokemonTypeDexCatalog {
            require(pcgEntries.isNotEmpty()) {
                "The canonical PCG catalog is empty"
            }

            val metadataByExactPcgName = metadataEntries
                .groupBy { metadata ->
                    PcgPokemonNameNormalizer.normalize(metadata.pcgName)
                }
                .mapNotNull { (key, candidates) ->
                    if (key.isBlank() || candidates.size != 1) {
                        null
                    } else {
                        key to candidates.single()
                    }
                }
                .toMap(LinkedHashMap())

            val canonicalEntries = pcgEntries.map { catalogEntry ->
                val canonicalKey = PcgPokemonNameNormalizer.normalize(
                    catalogEntry.displayName
                )

                require(canonicalKey.isNotBlank()) {
                    "PCG catalog contains a blank canonical name"
                }
                require(canonicalKey == catalogEntry.normalizedName) {
                    "PCG normalized name differs from its display name"
                }

                val metadata = metadataByExactPcgName[canonicalKey]
                val types = catalogEntry.types.toList()
                require(types.isNotEmpty()) {
                    "PCG catalog entry has no elemental type"
                }

                PokemonTypeEntry(
                    sourceKey = canonicalKey,
                    displayName = catalogEntry.displayName,
                    pcgName = catalogEntry.displayName,
                    tier = catalogEntry.tier,
                    type1 = types.first().displayName(),
                    type2 = types.getOrNull(1)?.displayName(),
                    weightKg = metadata?.weightKg,
                    baseSpeed = metadata?.baseSpeed,
                    baseHp = metadata?.baseHp,
                    evolvesTwice = metadata?.evolvesTwice,
                    aliases = buildList {
                        add(catalogEntry.displayName)
                        add(catalogEntry.normalizedName)
                        if (metadata != null) {
                            addAll(metadata.aliases)
                        }
                    }.filter(String::isNotBlank).distinct(),
                    mappingKind = metadata?.mappingKind ?: "pcg_catalog",
                    locked = metadata?.locked ?: false,
                    featured = metadata?.featured ?: false
                )
            }

            val byCanonicalName = canonicalEntries.associateByTo(
                LinkedHashMap(),
                PokemonTypeEntry::sourceKey
            )
            require(byCanonicalName.size == canonicalEntries.size) {
                "The canonical PCG catalog contains duplicate normalized names"
            }

            val aliasOwners = LinkedHashMap<String, MutableSet<String>>()
            for (entry in canonicalEntries) {
                for (alias in entry.aliases) {
                    val aliasKey = PcgPokemonNameNormalizer.normalize(alias)
                    if (aliasKey.isNotBlank()) {
                        aliasOwners
                            .getOrPut(aliasKey) { linkedSetOf() }
                            .add(entry.sourceKey)
                    }
                }
            }

            val byUniqueAlias = aliasOwners.mapNotNull { (aliasKey, owners) ->
                val ownerKey = owners.singleOrNull() ?: return@mapNotNull null
                val owner = byCanonicalName[ownerKey] ?: return@mapNotNull null
                aliasKey to owner
            }.toMap(LinkedHashMap())

            return PokemonTypeDexCatalog(
                entries = canonicalEntries,
                byCanonicalName = byCanonicalName,
                byUniqueAlias = byUniqueAlias
            )
        }

        private fun PcgPokemonType.displayName(): String {
            return name
                .lowercase(Locale.ROOT)
                .replaceFirstChar { character ->
                    character.titlecase(Locale.ROOT)
                }
        }
    }
}
