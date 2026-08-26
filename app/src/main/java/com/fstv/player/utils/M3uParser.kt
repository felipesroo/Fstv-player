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
        val channels = ArrayList<ChannelItem>(15000)
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768)

        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = "Geral"

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                val lastComma = trimmed.lastIndexOf(',')
                currentTitle = if (lastComma != -1 && lastComma < trimmed.length - 1) {
                    trimmed.substring(lastComma + 1).trim()
                } else {
                    "Canal sem Nome"
                }

                val gtIdx = trimmed.indexOf("group-title=\"")
                currentGroup = if (gtIdx != -1) {
                    val start = gtIdx + 13
                    val end = trimmed.indexOf('"', start)
                    if (end != -1) trimmed.substring(start, end).trim() else "Geral"
                } else {
                    "Geral"
                }
                if (currentGroup.isEmpty()) currentGroup = "Geral"

                val logoIdx = trimmed.indexOf("tvg-logo=\"")
                currentLogo = if (logoIdx != -1) {
                    val start = logoIdx + 10
                    val end = trimmed.indexOf('"', start)
                    if (end != -1) trimmed.substring(start, end).trim() else null
                } else null

            } else if (!trimmed.startsWith("#")) {
                val nameToUse = if (currentTitle.isNotEmpty()) currentTitle else "Canal ${channels.size + 1}"
                channels.add(
                    ChannelItem(
                        name = nameToUse,
                        streamUrl = trimmed,
                        logoUrl = currentLogo,
                        category = currentGroup
                    )
                )
                currentTitle = ""
                currentLogo = null
                currentGroup = "Geral"
            }
            line = reader.readLine()
        }
        return channels
    }

    fun parse(m3uContent: String): List<ChannelItem> {
        return parseStream(m3uContent.byteInputStream())
    }
}
