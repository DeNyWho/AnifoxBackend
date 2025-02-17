package club.anifox.backend.jpa.convert

import club.anifox.backend.domain.enums.user.StatusFavourite
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class StatusFavouriteConverter : AttributeConverter<StatusFavourite, String> {
    override fun convertToDatabaseColumn(attribute: StatusFavourite?): String {
        return attribute?.jsonKey ?: StatusFavourite.Watching.jsonKey
    }

    override fun convertToEntityAttribute(dbData: String?): StatusFavourite {
        return StatusFavourite.entries.find { it.jsonKey == dbData } ?: StatusFavourite.Watching
    }
}
