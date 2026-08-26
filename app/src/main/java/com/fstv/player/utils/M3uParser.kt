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
        val channels = ArrayList<ChannelItem>(20000)
        // Usar buffer expandido de 64KB para leitura máxima de velocidade
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 65536)

        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = "Geral"

        var line: String? = reader.readLine()
        while (line != null) {
            val len = line.length
            if (len == 0) {
                line = reader.readLine()
                continue
            }

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val lastComma = line.lastIndexOf(',')
                currentTitle = if (lastComma != -1 && lastComma < len - 1) {
                    line.substring(lastComma + 1).trim()
                } else {
                    "Canal sem Nome"
                }

                val gtIdx = line.indexOf("group-title=\"")
                currentGroup = if (gtIdx != -1) {
                    val start = gtIdx + 13
                    val end = line.indexOf('"', start)
                    if (end != -1) line.substring(start, end).trim() else "Geral"
                } else {
                    "Geral"
                }
                if (currentGroup.isEmpty()) currentGroup = "Geral"

                val logoIdx = line.indexOf("tvg-logo=\"")
                currentLogo = if (logoIdx != -1) {
                    val start = logoIdx + 10
                    val end = line.indexOf('"', start)
                    if (end != -1) line.substring(start, end).trim() else null
                } else null

            } else if (line[0] != '#') {
                val trimmedUrl = line.trim()
                if (trimmedUrl.isNotEmpty()) {
                    val nameToUse = if (currentTitle.isNotEmpty()) currentTitle else "Canal ${channels.size + 1}"
                    channels.add(
                        ChannelItem(
                            name = nameToUse,
                            streamUrl = trimmedUrl,
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
