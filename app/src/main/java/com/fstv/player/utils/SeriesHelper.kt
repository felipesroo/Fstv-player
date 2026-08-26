package com.fstv.player.utils

data class EpisodeItem(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val displayName: String,
    val item: ChannelItem
)

data class SeriesShowWithSeasons(
    val title: String,
    val logoUrl: String?,
    val category: String,
    val seasonsMap: Map<Int, List<EpisodeItem>> // Season -> Episodes
)

object SeriesHelper {

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

    fun extractSeasonAndEpisode(fullName: String): Pair<Int, Int> {
        var season = 1
        var episode = 1

        val sRegex = Regex("(?i)[ST](\\d{1,2})\\s*E(\\d{1,3})")
        val matchS = sRegex.find(fullName)
        if (matchS != null) {
            season = matchS.groupValues[1].toIntOrNull() ?: 1
            episode = matchS.groupValues[2].toIntOrNull() ?: 1
            return Pair(season, episode)
        }

        val xRegex = Regex("(?i)(\\d{1,2})x(\\d{1,3})")
        val matchX = xRegex.find(fullName)
        if (matchX != null) {
            season = matchX.groupValues[1].toIntOrNull() ?: 1
            episode = matchX.groupValues[2].toIntOrNull() ?: 1
            return Pair(season, episode)
        }

        val tempRegex = Regex("(?i)Temporada\\s*(\\d{1,2})")
        val matchTemp = tempRegex.find(fullName)
        if (matchTemp != null) {
            season = matchTemp.groupValues[1].toIntOrNull() ?: 1
        }

        val epRegex = Regex("(?i)E(?:pis[óo]dio)?\\s*(\\d{1,3})")
        val matchEp = epRegex.find(fullName)
        if (matchEp != null) {
            episode = matchEp.groupValues[1].toIntOrNull() ?: 1
        }

        return Pair(season, episode)
    }

    fun groupEpisodesByShowAndSeason(items: List<ChannelItem>): List<SeriesShowWithSeasons> {
        val showMap = LinkedHashMap<String, MutableList<ChannelItem>>()
        val logoMap = HashMap<String, String?>()
        val catMap = HashMap<String, String>()

        for (item in items) {
            val showTitle = extractShowTitle(item.name)
            var list = showMap[showTitle]
            if (list == null) {
                list = mutableListOf()
                showMap[showTitle] = list
                logoMap[showTitle] = item.logoUrl
                catMap[showTitle] = item.category
            }
            list.add(item)
        }

        return showMap.map { (title, rawEpisodes) ->
            val seasonsMap = LinkedHashMap<Int, MutableList<EpisodeItem>>()

            for (epItem in rawEpisodes) {
                val (season, epNum) = extractSeasonAndEpisode(epItem.name)
                var seasonList = seasonsMap[season]
                if (seasonList == null) {
                    seasonList = mutableListOf()
                    seasonsMap[season] = seasonList
                }

                val epCleanTitle = formatEpisodeCleanTitle(epItem.name, epNum)
                seasonList.add(
                    EpisodeItem(
                        seasonNumber = season,
                        episodeNumber = epNum,
                        displayName = epCleanTitle,
                        item = epItem
                    )
                )
            }

            val sortedSeasonsMap = seasonsMap.entries
                .sortedBy { it.key }
                .associate { entry ->
                    entry.key to entry.value.sortedBy { it.episodeNumber }
                }

            SeriesShowWithSeasons(
                title = title,
                logoUrl = logoMap[title],
                category = catMap[title] ?: "Séries",
                seasonsMap = sortedSeasonsMap
            )
        }
    }

    private fun formatEpisodeCleanTitle(fullName: String, epNum: Int): String {
        val dashIdx = fullName.indexOf(" - ")
        if (dashIdx > 0 && dashIdx + 3 < fullName.length) {
            val afterDash = fullName.substring(dashIdx + 3).trim()
            if (afterDash.isNotEmpty()) {
                return "EP %02d - %s".format(epNum, afterDash)
            }
        }
        return "EP %02d - %s".format(epNum, fullName)
    }
}
