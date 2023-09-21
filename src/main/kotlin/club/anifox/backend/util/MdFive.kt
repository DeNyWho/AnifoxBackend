package club.anifox.backend.util

import java.security.MessageDigest

fun mdFive(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray())

    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
