package com.example.backend.util.common

fun jikanThemesNormalize(input: String): String {
    println("INPUT = $input")
    val regex = "\"(.*)\" by (.*)".toRegex()
    val matchResult = regex.find(input.replace(Regex("\\(.*?\\)"), "").trim())
    val songTitle = matchResult?.groups?.get(1)?.value
    val artistName = matchResult?.groups?.get(2)?.value

    val artistNameParts = artistName?.split(" ") ?: emptyList()
    val formattedArtistName = if (artistNameParts.size >= 2) {
        val firstName = artistNameParts[0]
        val lastName = artistNameParts[1]
        "$lastName $firstName"
    } else {
        artistName
    }
    return "$formattedArtistName - $songTitle"
}