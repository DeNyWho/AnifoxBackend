package com.example.backend.util.common

fun checkEnglishLetter(word: String): Boolean {
    val regex = "^[A-Za-z]*$".toRegex()
    return regex.matches(word)
}