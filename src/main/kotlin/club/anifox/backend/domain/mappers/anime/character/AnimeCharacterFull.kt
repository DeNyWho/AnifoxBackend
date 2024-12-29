package club.anifox.backend.domain.mappers.anime.character

import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull
import club.anifox.backend.domain.model.anime.character.AnimeCharacterRole
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable

fun AnimeCharacterTable.toCharacterFull(): AnimeCharacterFull {
    return AnimeCharacterFull(
        id = id,
        name = name,
        nameEn = nameEn,
        nameKanji = nameKanji,
        image = image,
        aboutRu = aboutRu,
        pictures = pictures,
        roles = characterRoles.map { role ->
            AnimeCharacterRole(
                role = role.role,
                anime = role.anime.toAnimeLight(),
            )
        },
    )
}
