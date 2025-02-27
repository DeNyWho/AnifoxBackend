package club.anifox.backend.util.serializer

import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCountDefault
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCountSingle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.encodeToJsonElement

object AnimeTranslationCountSerializer : KSerializer<AnimeTranslationCount> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnimeTranslationCount")

    override fun serialize(encoder: Encoder, value: AnimeTranslationCount) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("Only JSON is supported")
        val json = jsonEncoder.json

        val jsonElement = when (value) {
            is AnimeTranslationCountDefault -> json.encodeToJsonElement(value)
            is AnimeTranslationCountSingle -> json.encodeToJsonElement(value)
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): AnimeTranslationCount {
        throw UnsupportedOperationException("Deserialization is not supported")
    }
}
