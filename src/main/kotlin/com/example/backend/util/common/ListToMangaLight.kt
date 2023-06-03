package com.example.backend.util.common

import com.example.backend.jpa.manga.MangaTable
import com.example.backend.models.mangaResponse.light.MangaLight

fun listToMangaLight(
    manga: List<MangaTable>
): List<MangaLight> {
    val mangaLight = mutableListOf<MangaLight>()
    manga.forEach {
        mangaLight.add(
            MangaLight(
                id = it.id.toString(),
                title = it.title,
                image = it.image,
            )
        )
    }
    return mangaLight
}