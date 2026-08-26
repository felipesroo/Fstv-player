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

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Extrair nome do canal (após a última vírgula)
                currentTitle = if (trimmed.contains(",")) {
                    trimmed.substringAfterLast(",").trim()
                } else {
                    "Canal sem Nome"
                }

                // Extrair Categoria / grupo
                currentGroup = when {
                    trimmed.contains("group-title=\"") -> {
                        trimmed.substringAfter("group-title=\"").substringBefore("\"").trim()
                    }
                    trimmed.contains("group-title='") -> {
                        trimmed.substringAfter("group-title='").substringBefore("'").trim()
                    }
                    else -> "Geral"
                }

                if (currentGroup.isEmpty()) currentGroup = "Geral"

                // Extrair Logo / Ícone
                currentLogo = when {
                    trimmed.contains("tvg-logo=\"") -> {
                        trimmed.substringAfter("tvg-logo=\"").substringBefore("\"").trim()
                    }
                    trimmed.contains("tvg-logo='") -> {
                        trimmed.substringAfter("tvg-logo='").substringBefore("'").trim()
                    }
                    else -> null
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // É a URL do canal/filme/série
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
