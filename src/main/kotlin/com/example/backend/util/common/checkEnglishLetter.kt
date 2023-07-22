package com.example.backend.util.common

fun checkEnglishLetter(word: String): Boolean {
    for (char in word) {
        if (char in 'a'..'z' || char in 'A'..'Z') {
            return true
        }
    }
    return false
}