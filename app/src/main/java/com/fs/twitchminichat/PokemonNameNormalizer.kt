package com.fs.twitchminichat

object PokemonNameNormalizer {

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""

        return raw
            .trim()
            .lowercase()
            .replace("é", "e")
            .replace("♀", "-f")
            .replace("♂", "-m")
            .replace(".", "")
            .replace("'", "")
            .replace(":", " ")
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("""\s+"""), " ")
            .let { normalizeRegionalForms(it) }
            .trim()
            .replace(" ", "-")
    }

    private fun normalizeRegionalForms(input: String): String {
        var s = input

        s = s.replace(Regex("""\balolan\b"""), "alola")
        s = s.replace(Regex("""\balola form\b"""), "alola")
        s = s.replace(Regex("""\bgalarian\b"""), "galar")
        s = s.replace(Regex("""\bgalar form\b"""), "galar")
        s = s.replace(Regex("""\bhisuian\b"""), "hisui")
        s = s.replace(Regex("""\bhisui form\b"""), "hisui")
        s = s.replace(Regex("""\bpaldean\b"""), "paldea")
        s = s.replace(Regex("""\bpaldea form\b"""), "paldea")

        s = s.replace(Regex("""\balola\s+([a-z0-9 -]+)"""), "$1 alola")
        s = s.replace(Regex("""\bgalar\s+([a-z0-9 -]+)"""), "$1 galar")
        s = s.replace(Regex("""\bhisui\s+([a-z0-9 -]+)"""), "$1 hisui")
        s = s.replace(Regex("""\bpaldea\s+([a-z0-9 -]+)"""), "$1 paldea")

        return s
    }
}