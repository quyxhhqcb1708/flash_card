package com.example.xq.flashcard.utils.ex

import java.text.Normalizer
import java.util.regex.Pattern

fun String?.deAccent(): String {
    val nfdNormalizedString: String = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(nfdNormalizedString).replaceAll("").replace("@", "")
}
