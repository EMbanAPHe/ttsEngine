package com.k2fsa.sherpa.onnx.tts.engine

/**
 * Minimal language/model descriptor used by UI lists.
 * If you already have an equivalent class, you can remove this
 * file and update imports to use the existing one.
 */
data class Lang(
    val code: String,
    val name: String,
    val voices: List<String> = emptyList()
)
