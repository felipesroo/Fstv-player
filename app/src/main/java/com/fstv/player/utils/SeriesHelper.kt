package com.fstv.player.utils

data class SeriesShow(
    val title: String,
    val logoUrl: String?,
    val category: String,
    val episodes: List<ChannelItem>
)

object SeriesHelper {

    private val seriesSeasonEpisodeRegex = Regex(
        "(?i)^(.*?)\\s+([ST]\\d{1,2}\\s*E\\d{1,3}.*|S\\d{1,2}\\b.*|T\\d{1,2}\\b.*|E\\d{1,3}\\b.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extrai o nome da série limpo sem S01E01 ou número de temporada/episódio.
     * Exemplo: "Amor Moderno S01 Amor Moderno - S01E01 - Quando o..." -> "Amor Moderno"
     * Exemplo: "Stranger Things S04E01 - Capitulo Um" -> "Stranger Things"
     */
    fun extractShowTitle(fullName: String): String {
        val trimmed = fullName.trim()

        val match = seriesSeasonEpisodeRegex.find(trimmed)
        if (match != null) {
            val titleCandidate = match.groupValues[1].trim()
                .trimEnd('-', ':', '|', ' ')
            if (titleCandidate.isNotEmpty() && titleCandidate.length > 2) {
                return titleCandidate
            }
        }

        // Tentar separar por traço ou S01/T01
        val splitByS = trimmed.split(Regex("(?i)\\s+[ST]\\d{1,2}"))
        if (splitByS.size > 1 && splitByS[0].trim().isNotEmpty()) {
            return splitByS[0].trim().trimEnd('-', ':', '|', ' ')
        }

        val splitByDash = trimmed.split(" - ")
        if (splitByDash.size > 1) {
            return splitByDash[0].trim()
        }

        return trimmed
    }

    /**
     * Agrupa uma lista de episódios avulsos por Séries (Show).
     */
    fun groupEpisodesByShow(items: List<ChannelItem>): List<SeriesShow> {
        val map = LinkedHashMap<String, MutableList<ChannelItem>>()
        val logoMap = HashMap<String, String?>()
        val catMap = HashMap<String, String>()

        for (item in items) {
            val showTitle = extractShowTitle(item.name)
            if (!map.containsKey(showTitle)) {
                map[showTitle] = mutableListOf()
                logoMap[showTitle] = item.logoUrl
                catMap[showTitle] = item.category
            }
            map[showTitle]!!.add(item)
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
