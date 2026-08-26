package com.fstv.player.utils

data class SeriesShow(
    val title: String,
    val logoUrl: String?,
    val category: String,
    val episodes: List<ChannelItem>
)

object SeriesHelper {

    /**
     * Extrai o nome da série limpo de forma ultra-rápida sem usar Regex pesado no loop principal.
     */
    fun extractShowTitle(fullName: String): String {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return "Série"

        val len = trimmed.length
        var cutIndex = -1

        for (i in 0 until len - 2) {
            val c = trimmed[i]
            if ((c == 'S' || c == 's' || c == 'T' || c == 't') && trimmed[i + 1].isDigit()) {
                if (i == 0 || trimmed[i - 1] == ' ' || trimmed[i - 1] == '-' || trimmed[i - 1] == '.' || trimmed[i - 1] == '[') {
                    cutIndex = i
                    break
                }
            }
        }

        if (cutIndex > 2) {
            val candidate = trimmed.substring(0, cutIndex).trim().trimEnd('-', ':', '|', ' ', '.')
            if (candidate.isNotEmpty()) return candidate
        }

        val dashIdx = trimmed.indexOf(" - ")
        if (dashIdx > 2) {
            return trimmed.substring(0, dashIdx).trim()
        }

        return trimmed
    }

    /**
     * Agrupa episódios por Série.
     */
    fun groupEpisodesByShow(items: List<ChannelItem>): List<SeriesShow> {
        val map = LinkedHashMap<String, MutableList<ChannelItem>>()
        val logoMap = HashMap<String, String?>()
        val catMap = HashMap<String, String>()

        for (item in items) {
            val showTitle = extractShowTitle(item.name)
            var list = map[showTitle]
            if (list == null) {
                list = mutableListOf()
                map[showTitle] = list
                logoMap[showTitle] = item.logoUrl
                catMap[showTitle] = item.category
            }
            list.add(item)
        }

        return map.map { (title, episodes) ->
            SeriesShow(
                title = title,
                logoUrl = logoMap[title],
                category = catMap[title] ?: "Séries",
                episodes = episodes
            )
        }
    }
}
