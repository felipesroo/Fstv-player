package com.fstv.player.utils

data class ChannelItem(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "Geral"
)

object M3uParser {

    fun parse(m3uContent: String): List<ChannelItem> {
        val channels = mutableListOf<ChannelItem>()
        val lines = m3uContent.lines()

        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = "Geral"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse EXTINF metadata
                currentTitle = trimmed.substringAfterLast(",").trim()

                // Extract group-title if present
                if (trimmed.contains("group-title=\"")) {
                    currentGroup = trimmed.substringAfter("group-title=\"").substringBefore("\"")
                } else {
                    currentGroup = "Geral"
                }

                // Extract tvg-logo if present
                if (trimmed.contains("tvg-logo=\"")) {
                    currentLogo = trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
                } else {
                    currentLogo = null
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // Stream URL line
                if (currentTitle.isNotEmpty()) {
                    channels.add(
                        ChannelItem(
                            name = currentTitle,
                            streamUrl = trimmed,
                            logoUrl = currentLogo,
                            category = currentGroup
                        )
                    )
                    currentTitle = ""
                    currentLogo = null
                    currentGroup = "Geral"
                }
            }
        }
        return channels
    }
}
