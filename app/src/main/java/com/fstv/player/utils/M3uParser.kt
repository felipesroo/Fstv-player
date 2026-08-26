package com.fstv.player.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ChannelItem(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "Geral"
)

object M3uParser {

    fun parseStream(inputStream: InputStream): List<ChannelItem> {
        val channels = mutableListOf<ChannelItem>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = "Geral"

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentTitle = trimmed.substringAfterLast(",").trim()

                currentGroup = if (trimmed.contains("group-title=\"")) {
                    trimmed.substringAfter("group-title=\"").substringBefore("\"")
                } else {
                    "Geral"
                }

                currentLogo = if (trimmed.contains("tvg-logo=\"")) {
                    trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
                } else {
                    null
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
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
            line = reader.readLine()
        }
        return channels
    }

    fun parse(m3uContent: String): List<ChannelItem> {
        return parseStream(m3uContent.byteInputStream())
    }
}
