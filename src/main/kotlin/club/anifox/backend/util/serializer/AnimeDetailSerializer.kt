package club.anifox.backend.util.serializer

import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.detail.AnimeDetailDefault
import club.anifox.backend.domain.model.anime.detail.AnimeDetailWithUser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.encodeToJsonElement

object AnimeDetailSerializer : KSerializer<AnimeDetail> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnimeDetail")

    override fun serialize(encoder: Encoder, value: AnimeDetail) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("Only JSON is supported")
        val json = jsonEncoder.json

        val jsonElement = when (value) {
            is AnimeDetailDefault -> json.encodeToJsonElement(value)
            is AnimeDetailWithUser -> json.encodeToJsonElement(value)
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): AnimeDetail {
        throw UnsupportedOperationException("Deserialization is not supported")
    }
}
